(ns votingbuddy.views.home
  (:require
   [re-frame.core :as rf]
   [votingbuddy.endorsements :as endorse]
   [votingbuddy.auth :as auth]))

(defn home []
  (.log js/console "In home view fn")

  (let [endorsements (rf/subscribe [:endorsements/list])]
    (fn []
      [:div.content>div.columns.is-centered>div.column.is-two-thirds
       [:div.columns
        [endorse/search-endorsements]
        ]
       (let [loading? (rf/subscribe [:endorsements/loading?])]
         (when (not @loading?)
           [:div.column
            [endorse/endorsement-list endorsements]]))
       [:div.columns>div.column
        [endorse/reload-endorsements-button]]
       [:div.columns>div.column
        (case @(rf/subscribe [:auth/user-state])
          :loading
          [:div {:style {:width "5em"}}
           [:progress.progress.is-dark.is-small {:max 100} "30%"]]
          :authenticated
          [endorse/endorsement-form]
          :anonymous
          [:div.notification.is-clearfix
           [:span "Log in or create an account to post an endorsement!"]
           [:div.buttons.is-pulled-right
            [auth/login-button]
            [auth/register-button]]])]])))


;; (defn home []
;;     (.log js/console "In home view fn")

;;   (let [endorsements (rf/subscribe [:endorsements/list])]
;;     (fn []
;;       [:div.content>div.columns.is-centered>div.column.is-two-thirds
;;        [:div.columns>div.column
;;         [:h3 "Endorsements"]
;;         [endorse/endorsement-list endorsements]]
;;        [:div.columns>div.column
;;         [endorse/reload-endorsements-button]]
;;        [:div.columns>div.column
;;         (case @(rf/subscribe [:auth/user-state])
;;           :loading
;;           [:div {:style {:width "5em"}}
;;            [:progress.progress.is-dark.is-small {:max 100} "30%"]]
;;           :authenticated
;;           [endorse/endorsement-form]
;;           :anonymous
;;           [:div.notification.is-clearfix
;;            [:span "Log in or create an account to post an endorsement!"]
;;            [:div.buttons.is-pulled-right
;;             [auth/login-button]
;;             [auth/register-button]]])]])))

(def home-controllers
  [{:start (fn [_] (.log js/console "In home controller"))}])

;; (def home-controllers
;;   [{:start (fn [_] (rf/dispatch [:endorsements/load]))}])
