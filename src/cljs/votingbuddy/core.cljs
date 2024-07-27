(ns votingbuddy.core
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [re-frame.core :as rf]
            [reitit.coercion.spec :as reitit-spec]
            [reitit.frontend :as rtf]
            [reitit.frontend.easy :as rtfe]
            [clojure.string :as string]
            [reitit.frontend.controllers :as rtfc]
            [votingbuddy.routes.app :refer [app-routes]]
            [votingbuddy.websockets :as ws]
            [votingbuddy.auth :as auth]
            [votingbuddy.endorsements :as endorse]
            [votingbuddy.ajax :as ajax]
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
   {:db {:session/loading? true}
    :dispatch [:session/load]}))

(def router
  (rtf/router
   (app-routes)
   {:data {:coercion reitit-spec/coercion}}))

(rf/reg-event-db
 :router/navigated
 (fn [db [_ new-match]]
   (assoc db :router/current-route new-match)))

(rf/reg-sub
 :router/current-route
 (fn [db]
   (:router/current-route db)))


(defn init-routes! []
  (rtfe/start!
   router
   (fn [new-match]
     (when new-match
       (let [{controllers :controllers}
             @(rf/subscribe [:router/current-route])
             new-match-with-controllers
             (assoc new-match
                    :controllers
                    (rtfc/apply-controllers controllers new-match))]
         (rf/dispatch [:router/navigated new-match]))))
   {:use-fragment false}))

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
           "Home"]
          (when (= @(rf/subscribe [:auth/user-state]) :authenticated)
            [:a.navbar-item
             {:href (rtfe/href :votingbuddy.routes.app/author
                               {:user (:login @(rf/subscribe [:auth/user]))})}
             "This org's Endorsements"])]
         [:div.navbar-end
          [:div.navbar-item
           (case @(rf/subscribe [:auth/user-state])
             :loading
             [:div {:style {:width "5em"}}
              [:progress.progress.is-dark.is-small {:max 100} "30%"]]
             :authenticated
             [:div.buttons
              [auth/nameplate @(rf/subscribe [:auth/user])]
              [auth/logout-button]]
             :anonymous
             [:div.buttons
              [auth/login-button]
              [auth/register-button]])]]]]])))

(defn page [{{:keys [view name]} :data
             path                :path
             :as                 match}]
  (.log js/console "In page! view/name are: ")
  ;; (defn author [{{{:keys [user]} :path} :parameters :as i}]

  [:section.section>div.container
   (if view
     [view match]
     [:div "No view specified for route: " name " (" path ")"])])

(defn app []
  (let [current-route @(rf/subscribe [:router/current-route])]
    [:div.app
     [navbar]
     [page current-route]]))

(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (.log js/console "Mounting components...")
  (init-routes!)
  (dom/render [#'app] (.getElementById js/document "content"))
  (.log js/console "Components Mounted!"))

(defn init! []
  (.log js/console "Initializing app")
  (mount/start)
  (rf/dispatch-sync [:app/initialize])
  (mount-components))

