(ns votingbuddy.endorsements
  (:require
   [clojure.string :as string]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [votingbuddy.validation :refer [validate-endorsement]]))

(rf/reg-event-fx
 :endorsements/load
 (fn [{:keys [db]} _]
   ;; early version
  ;;  (GET "/api/endorsements"
  ;;    {:headers {"Accept" "application/transit+json"}
  ;;     :handler #(rf/dispatch [:endorsements/set (:endorsements %)])})
   {:db (assoc db
               :endorsements/loading? true
               :endorsements/filter nil)
    :ajax/get {:url "/api/endorsements"
               :success-path [:endorsements]
               :success-event [:endorsements/set]}}))

(rf/reg-event-fx
 :endorsements/load-by-author
 (fn [{:keys [db]} [_ author]]
   {:db (-> db
            (assoc :endorsements/loading? true
                   :endorsements/filter {:author author}))
    :ajax/get {:url (str "/api/endorsements/byauthor/" author)
               :success-path [:endorsements]
               :success-event [:endorsements/set]}}))

(rf/reg-event-fx
 :endorsements/load-by-address
 (fn [{:keys [db]} [_ address]]
   {:db (-> db
            (assoc :endorsements/loading? true
                   :endorsements/filter nil))
    :ajax/get {:url (str "/api/endorsements/byaddress/" address)
               :success-path [:endorsements]
               :success-event [:endorsements/set]}}))

(rf/reg-event-db
 :endorsements/set
 (fn [db [_ endorsements]]
   (-> db
       (assoc :endorsements/loading? false
              :endorsements/list endorsements))))

(rf/reg-sub
 :endorsements/loading?
 (fn [db _]
   (:endorsements/loading? db)))

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

(defn endorsement-list [endorsements]
  [:ul.endorsements
   (for [{:keys [candidatename timestamp subject statement orgname]} @endorsements]
     ^{:key timestamp}
     [:li
      [:time (.toLocaleString timestamp)]
      [:p "Endorsement for " candidatename]
      [:p subject]
      [:p statement]
      [:p " - " orgname
       " <"
       [:a {:href (str "/user/" orgname)} (str "@" orgname)]
       ">"]])])

(defn add-endorsement? [filter-map endorsement]
  (every?
   (fn [[k matcher]]
     (let [v (get endorsement k)]
       (cond
         (set? matcher)
         (matcher v)
         (fn? matcher)
         (matcher v)
         :else
         (= matcher v))))
   filter-map))

(rf/reg-event-db
 :endorsement/add
 (fn [db [_ endorsement]]
   (.log js/console "Server replied after Posting to server!!")
   (if (add-endorsement? (:endorsements/filter db) endorsement)
     (update db :endorsements/list conj endorsement)
     db)))

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


(defn errors-component [id & [message]]
  (when-let [error @(rf/subscribe [:form/error id])]
    [:div.notification.is-danger (if message
                                   message
                                   (string/join error))]))

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
   [errors-component :unauthorized "Please log in before posting."]
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


(defn search-endorsements []
  (fn []
    [:div.column
     [:h3 "Enter your address to see who's running in the next election"]
     [:br]
     [:div.field
      [:label.label {:for :address} "Address"]
      [text-input {:attrs {:name :address}
                   :value (rf/subscribe [:form/field :address])
                   :on-save #(rf/dispatch [:form/set-field :address %])}]
      
    ;;   [:input#address.input]
      ]
     [:input.button.is-primary
      {:type :submit
       :on-click
    ;;   #(rf/dispatch [:message/send!
    ;;                            @(rf/subscribe [:form/fields])])
       #(rf/dispatch [:endorsements/load-by-address @(rf/subscribe [:form/field :address]) ])

       :value "Search"}]]))

;; [:button.button.is-primary
;;  {:on-click #(POST "/api/logout"
;;                {:handler (fn [_]
;;                            (.log js/console "hi"))})}
;;  "Search"]

;; [:button.button.is-info.is-fullwidth
;;  {:on-click #(rf/dispatch [:endorsements/load])
;;   :disabled @loading?}
;;  (if @loading?
;;    "Loading Endorsements"
;;    "Refresh Endorsements")]

;;  [:button.button
;;   {:on-click #(POST "/api/logout"
;;                 {:handler (fn [_]
;;                             (rf/dispatch [:auth/handle-logout]))})}
;;   "Log Out"])




