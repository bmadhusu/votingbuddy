(ns votingbuddy.core
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [re-frame.core :as rf]
            [ajax.core :refer [GET POST]]
            [clojure.string :as string]
            [votingbuddy.validation :refer [validate-endorsement]]
            [votingbuddy.websockets :as ws]
            [mount.core :as mount]))

;; below is old school way of using cjscript
;; (-> (.getElementById js/document "content")
;;     (.-innerHTML)
;;     (set! "Hello, Auto"))

;; (dom/render
;;  [:div#hello.content>h1 "Hello, ReagentYo"]
;;  (.getElementById js/document "content"))

(rf/reg-event-fx
 :app/initialize
 (fn [_ _]
   {:db {:endorsements/loading? true
         :session/loading? true}
    :dispatch-n [[:session/load] [:endorsements/load]]}))



(rf/reg-event-db
 :session/set
 (fn [db [_ {:keys [identity]}]]
   (assoc db
          :auth/user identity
          :session/loading? false)))

(rf/reg-sub
 :session/loading?
 (fn [db _]
   (:session/loading? db)))

(rf/reg-fx
 :ajax/get
 (fn [{:keys [url success-event error-event success-path]}]
   (GET url
     (cond-> {:headers {"Accept" "application/transit+json"}}
       success-event (assoc :handler
                            #(rf/dispatch
                              (conj success-event
                                    (if success-path
                                      (get-in % success-path)
                                      %))))
       error-event (assoc :error-handler
                          #(rf/dispatch (conj error-event %)))))))



(rf/reg-event-fx
 :endorsements/load
 (fn [{:keys [db]} _]
   ;; early version
  ;;  (GET "/api/endorsements"
  ;;    {:headers {"Accept" "application/transit+json"}
  ;;     :handler #(rf/dispatch [:endorsements/set (:endorsements %)])})
   {:db (assoc db :endorsements/loading? true)
    :ajax/get {:url "api/endorsements"
               :success-path [:endorsements]
               :success-event [:endorsements/set]}}))

(rf/reg-event-fx
 :session/load
 (fn [{:keys [db]} _]
   {:db (assoc db :session/loading? true)
    :ajax/get {:url "/api/session"
               :success-path [:session]
               :success-event [:session/set]}}))

(rf/reg-sub
 :endorsements/loading?
 (fn [db _]
   (:endorsements/loading? db)))


(rf/reg-event-db
 :endorsement/add
 (fn [db [_ endorsement]]
   (.log js/console "Server replied after Posting to server!!");;
   (update db :endorsements/list conj endorsement)))

(rf/reg-event-fx
 :message/send!-called-back
 (fn [_ [_ {:keys [success errors]}]]
   (if success
     {:dispatch [:form/clear-fields]}
     {:dispatch [:form/set-server-errors errors]})))

(rf/reg-event-fx
 :message/send!
 (fn [{:keys [db]} [_ fields]]
  ;;  (POST "/api/endorsement"
  ;;    {:format :json
  ;;     :headers
  ;;     {"Accept" "application/transit+json"
  ;;      "x-csrf-token" (.-value (.getElementById js/document "token"))}
  ;;     :params fields
  ;;     :handler #(rf/dispatch
  ;;                [:message/add
  ;;                 (-> fields
  ;;                     (assoc :timestamp (js/Date.)))])
  ;;     :error-handler #(rf/dispatch
  ;;                      [:form/set-server-errors
  ;;                       (get-in % [:response :errors])])})

   ;; below is 2nd version; commenting out
  ;;  (ws/send! [:endorsement/create! fields]
  ;;            10000
  ;;            (fn [{:keys [success errors] :as response}]
  ;;              (.log js/console "Called back: " (pr-str response))
  ;;              (if success
  ;;                (rf/dispatch [:form/clear-fields])
  ;;                (rf/dispatch [:form/set-server-errors errors]))))
   {:db (dissoc db :form/server-errors)
    :ws/send! {:message [:endorsement/create! fields]
               :timeout 10000
               :callback-event [:message/send!-called-back]}}))

(defn handle-response! [response]
  (if-let [errors (:errors response)]
    (rf/dispatch [:form/set-server-errors errors])
    (do
      (rf/dispatch [:endorsement/add response])
      (rf/dispatch [:form/clear-fields response]))))

(defn send-endorsement! [fields errors]
  (if-let [validation-errors (validate-endorsement @fields)]
    (do
      (.log js/console "failed validation!")
      (reset! errors validation-errors))
    (POST "/api/endorsement"
      {:format :json
       :headers
       {"Accept" "application/transit+json"
        "x-csrf-token" (.-value (.getElementById js/document "token"))}
       :params @fields
       :handler (fn [_]
                  (rf/dispatch
                   [:endorsement/add (assoc @fields :timestamp (js/Date.))])
                  (reset! fields nil)
                  (reset! errors nil))
       :error-handler (fn [e]
                        (.log js/console (str e))
                        (reset! errors (-> e :response :errors)))})))



(defn endorsement-list [endorsements]
  (println endorsements)
  [:ul.endorsements
   (for [{:keys [timestamp subject statement name]} @endorsements]
     ^{:key timestamp}
     [:li
      [:time (.toLocaleString timestamp)]
      [:p subject]
      [:p statement]
      [:p " - " name]])])



(rf/reg-event-db
 :form/set-field
 [(rf/path :form/fields)]
 (fn [fields [_ id value]]
   (assoc fields id value)))

(rf/reg-event-db
 :form/clear-fields
 [(rf/path :form/fields)]
 (fn [_ _]
   {}))

(rf/reg-sub
 :form/fields
 (fn [db _]
   (:form/fields db)))

(rf/reg-sub
 :form/field
 :<- [:form/fields]
 (fn [fields [_ id]]
   (get fields id)))

(rf/reg-event-db
 :form/set-server-errors
 [(rf/path :form/server-errors)]
 (fn [_ [_ errors]]
   errors))

(rf/reg-sub
 :form/server-errors
 (fn [db _]
   (:form/server-errors db)))

;;Validation errors are reactively computed
(rf/reg-sub
 :form/validation-errors
 :<- [:form/fields]
 (fn [fields _]
   (validate-endorsement fields)))

(rf/reg-sub
 :form/validation-errors?
 :<- [:form/validation-errors]
 (fn [errors _]
   (not (empty? errors))))

(rf/reg-sub
 :form/errors
 :<- [:form/validation-errors]
 :<- [:form/server-errors]
 (fn [[validation server] _]
   (merge validation server)))

(rf/reg-sub
 :form/error
 :<- [:form/errors]
 (fn [errors [_ id]]
   (get errors id)))

(defn errors-component [id]
  (when-let [error @(rf/subscribe [:form/error id])]
    [:div.notification.is-danger (string/join error)]))

(defn text-input [{val    :value
                   attrs  :attrs
                   :keys  [on-save]}]
  (let [draft (r/atom nil)
        value (r/track #(or @draft @val ""))]
    (fn []
      [:input.input
       (merge attrs
              {:type  :text
               :on-focus #(reset! draft (or @val ""))
               :on-blur (fn []
                          (on-save (or @draft ""))
                          (reset! draft nil))
               :on-change #(reset! draft (.. % -target -value))
               :value @value})])))

(defn textarea-input [{val   :value
                       attrs :attrs
                       :keys [on-save]}]
  (let [draft (r/atom nil)
        value (r/track #(or @draft @val ""))]
    (fn []
      [:textarea.textarea
       (merge attrs
              {:on-focus #(reset! draft (or @val ""))
               :on-blur (fn []
                          (on-save (or @draft ""))
                          (reset! draft nil))
               :on-change #(reset! draft (.. % -target -value))
               :value @value})])))

(defn endorsement-form []
  [:div
   [errors-component :server-error]
   [:div.field
    [:label.label {:for :candidate} "Candidate"]
    [errors-component :candidate]
    ;; [:input.input
    ;;  {:type :text
    ;;   :name :subject
    ;;   :on-change #(rf/dispatch
    ;;                [:form/set-field
    ;;                 :name
    ;;                 (.. % -target -value)])
    ;;   :value @(rf/subscribe [:form/field :name])}]
    [text-input {:attrs {:name :candidate}
                 :value (rf/subscribe [:form/field :candidate])
                 :on-save #(rf/dispatch [:form/set-field :candidate %])}]]
   [:div.field
    [:label.label {:for :subject} "Subject"]
    [errors-component :subject]
    ;; [:textarea.textarea
    ;;  {:name :subject
    ;;   :value @(rf/subscribe [:form/field :subject])
    ;;   :on-change #(rf/dispatch
    ;;                [:form/set-field
    ;;                 :subject
    ;;                 (.. % -target -value)])}]
    [textarea-input {:attrs {:name :subject}
                     :value (rf/subscribe [:form/field :subject])
                     :on-save #(rf/dispatch [:form/set-field :subject %])}]]
   [:div.field
    [:label.label {:for :statement} "Statement"]
    [errors-component :statement]
    ;; [:textarea.textarea
    ;;  {:name :statement
    ;;   :value @(rf/subscribe [:form/field :statement])
    ;;   :on-change #(rf/dispatch
    ;;                [:form/set-field
    ;;                 :statement
    ;;                 (.. % -target -value)])}]
    [textarea-input {:attrs {:name :statement}
                     :value (rf/subscribe [:form/field :statement])
                     :on-save #(rf/dispatch [:form/set-field :statement %])}]]


   [:input.button.is-primary
    {:type :submit
     :disabled @(rf/subscribe [:form/validation-errors?])
     :on-click #(rf/dispatch [:message/send!
                              @(rf/subscribe [:form/fields])])
     :value "comment"}]])

(rf/reg-event-db
 :endorsements/set
 (fn [db [_ endorsements]]
   (-> db
       (assoc :endorsements/loading? false
              :endorsements/list endorsements))))

(rf/reg-sub
 :endorsements/list
 (fn [db _]
   (:endorsements/list db [])))

(defn reload-endorsements-button []
  (let [loading? (rf/subscribe [:endorsements/loading?])]
    [:button.button.is-info.is-fullwidth
     {:on-click #(rf/dispatch [:endorsements/load])
      :disabled @loading?}
     (if @loading?
       "Loading Endorsements"
       "Refresh Endorsements")]))

(defn home []
  (let [endorsements (rf/subscribe [:endorsements/list])]
    (fn []
      [:div.content>div.columns.is-centered>div.column.is-two-thirds
       [:div.columns>div.column
        [:h3 "Endorsements"]
        [endorsement-list endorsements]]
       [:div.columns>div.column
        [reload-endorsements-button]]
       [:div.columns>div.column
        [endorsement-form]]])))

(rf/reg-event-db
 :app/show-modal
 (fn [db [_ modal-id]]
   (assoc-in db [:app/active-modals modal-id] true)))

(rf/reg-event-db
 :app/hide-modal
 (fn [db [_ modal-id]]
   (update db :app/active-modals dissoc modal-id)))

(rf/reg-sub
 :app/active-modals
 (fn [db _]
   (:app/active-modals db {})))

(rf/reg-sub
 :app/modal-showing?
 :<- [:app/active-modals]
 (fn [modals [_ modal-id]]
   (get modals modal-id false)))

(defn modal-card [id title body footer]
  [:div.modal
   {:class (when @(rf/subscribe [:app/modal-showing? id])
             "is-active")}
   [:div.modal-background
    {:on-click #(rf/dispatch [:app/hide-modal id])}]
   [:div.modal-card
    [:header.modal-card-head
     [:p.modal-card-title title]
     [:button.delete
      {:on-click #(rf/dispatch [:app/hide-modal id])}]]
    [:section.modal-card-body body]
    [:footer.modal-card-foot footer]]])

(defn modal-button [id title body footer]
  [:div
   [:button.button.is-primary
    {:on-click #(rf/dispatch [:app/show-modal id])} title]
   [modal-card id title body footer]])

(rf/reg-event-db
 :auth/handle-login
 (fn [db [_ {:keys [identity]}]]
   (assoc db :auth/user identity)))

(rf/reg-event-db
 :auth/handle-logout
 (fn [db _]
   (dissoc db :auth/user)))

(rf/reg-sub
 :auth/user
 (fn [db _]
   (:auth/user db)))


(defn login-button []
  (r/with-let
    [fields (r/atom {})
     error (r/atom nil)
     do-login (fn [_]
                (reset! error nil)
                (POST "/api/login"
                  {:headers {"Accept" "application/transit+json"}
                   :params @fields
                   :handler (fn [response]
                              (reset! fields {})
                              (rf/dispatch [:auth/handle-login response])
                              (rf/dispatch [:app/hide-modal :user/login]))
                   :error-handler (fn [error-response]
                                    (reset! error
                                            (or (:message (:response error-response))
                                                (:status-text error-response)
                                                "Unknown Error")))}))]
    [modal-button :user/login
     ;;Title
     "Log In"
     ;;Body
     [:div (when-not (string/blank? @error)
             [:div.notification.is-danger @error])
      [:div.field
       [:div.label "Login"]
       [:div.control
        [:input.input
         {:type "text"
          :value (:login @fields)
          :on-change #(swap! fields assoc :login (.. % -target -value))}]]]
      [:div.field
       [:div.label "Password"]
       [:div.control
        [:input.input
         {:type "password"
          :value (:password @fields)
          :on-change #(swap! fields assoc :password (.. % -target -value))
          ;; Submit login form when Enter key is pressed
          :on-key-down #(when (= (.-keyCode %) 13)
                          (do-login))}]]]]
     ;; Footer
     [:button.button.is-primary.is-fullwidth
      {:on-click do-login
       :disabled (or (string/blank? (:login @fields))
                     (string/blank? (:password @fields)))}
      "Log In"]]))

(defn logout-button []
  [:button.button
   {:on-click #(POST "/api/logout"
                 {:handler (fn [_]
                             (rf/dispatch [:auth/handle-logout]))})}
   "Log Out"])

(defn nameplate [{:keys [login]}]
  [:button.button.is-primary login])

(defn register-button []
  (r/with-let
    [fields (r/atom {})
     error (r/atom nil)
     do-register (fn [_]
                   (reset! error nil)
                   (POST "/api/register"
                     {:headers {"Accept" "application/transit+json"}
                      :params @fields
                      :handler (fn [response] (reset! fields {})
                                 (rf/dispatch [:app/hide-modal :user/register])
                                 (rf/dispatch [:app/show-modal :user/login]))
                      :error-handler (fn [error-response]
                                       (reset! error (or (:message
                                                          (:response error-response))
                                                         (:status-text error-response)
                                                         "Unknown error")))}))]
    [modal-button :user/register
     ;; Title
     "Create Account"
     ;; Body
     [:div
      (when-not (string/blank? @error)
        [:div.notification.is-danger @error])
      [:div.field
       [:div.label "Login"]
       [:div.control
        [:input.input
         {:type "text"
          :value (:login @fields)
          :on-change #(swap! fields assoc :login (.. % -target -value))}]]]
      [:div.field
       [:div.label "Password"]
       [:div.control
        [:input.input
         {:type "password"
          :value (:password @fields)
          :on-change #(swap! fields assoc :password (.. % -target -value))}]]]
      [:div.field
       [:div.label "Confirm Password"]
       [:div.control
        [:input.input
         {:type "password"
          :value (:confirm @fields)
          :on-change #(swap! fields assoc :confirm (.. % -target -value))
          ;;Submit login form when Enter is pressed
          :on-key-down #(when (= (.-keyCode %) 13) (do-register))}]]]]
     ;; Footer
     [:button.button.is-primary.is-fullwidth
      {:on-click do-register
       :disabled (or (string/blank? (:login @fields))
                     (string/blank? (:password @fields))
                     (string/blank? (:confirm @fields)))}
      "Create Account"]]))

(rf/reg-sub
 :auth/user-state
 :<- [:auth/user]
 :<- [:session/loading?]
 (fn [[user loading?]]
   (cond
     (true? loading?) :loading
     user :authenticated
     :else :anonymous)))

(defn navbar []
  (let [burger-active (r/atom false)]
    (fn []
      [:nav.navbar.is-info
       [:div.container
        [:div.navbar-brand
         [:a.navbar-item
          {:href "/"
           :style {:font-weight "bold"}}
          "votingbuddy"]
         [:span.navbar-burger.burger
          {:data-target "nav-menu"
           :on-click #(swap! burger-active not)
           :class (when @burger-active "is-active")}
          [:span]
          [:span]
          [:span]]]
        [:div#nav-menu.navbar-menu
         {:class (when @burger-active "is-active")}
         [:div.navbar-start
          [:a.navbar-item
           {:href "/"}
           "Home"]]
         [:div.navbar-end
          [:div.navbar-item
           (case @(rf/subscribe [:auth/user-state])
             :loading
             [:div {:style {:width "5em"}}
              [:progress.progress.is-dark.is-small {:max 100} "30%"]]
             :authenticated
             [:div.buttons
              [nameplate @(rf/subscribe [:auth/user])]
              [logout-button]]
             :anonymous
               [:div.buttons
                [login-button]
                [register-button]])]]]]])))


(defn app []
  [:div.app
   [navbar]
   [:section.section
    [:div.container
     [home]]]])

(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (.log js/console "Mounting components...")
  (dom/render [#'app] (.getElementById js/document "content"))
  (.log js/console "Components Mounted!"))

(defn init! []
  (.log js/console "Initializing app")
  (mount/start)
  (rf/dispatch [:app/initialize])
  (mount-components))





;; (def a {:b 1 :app/active-modals [3]})
;; (def a (assoc-in nil [:app/active-modals 3] true))
;; (update a :app/active-modals dissoc 2)


;

