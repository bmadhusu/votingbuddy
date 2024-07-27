(ns votingbuddy.views.author
  (:require
   [re-frame.core :as rf]
   [votingbuddy.endorsements :as endorse]))

(defn author [{{{:keys [user]} :path} :parameters :as i} ]
    (.log js/console "In author view fn")

  (let [endorsements (rf/subscribe [:endorsements/list])]
    (fn [{{{:keys [user]} :path} :parameters}]
      [:div.content>div.columns.is-centered>div.column.is-two-thirds
       [:div.columns>div.column
        [:h3 "Endorsements By " user]
        [endorse/endorsement-list endorsements]]])))

(def author-controllers
  [{:parameters {:path [:user]}
    :start (fn [{{:keys [user]} :path}]
             (rf/dispatch [:endorsements/load-by-author user]))}])


