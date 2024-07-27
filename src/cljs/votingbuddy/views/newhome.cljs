(ns votingbuddy.views.newhome
  (:require
   [re-frame.core :as rf]
   [votingbuddy.endorsements :as endorse]
   [votingbuddy.auth :as auth]
   [reagent.core :as r]))

(defn home []
  (.log js/console "In newhome view fn")
  (fn [_]
    [:div
     [:h1 "Enter your address to see who's running in the next election"]
     [:br]
     [:div.field
      [:label.label {:for :address} "Address"]
      [:input#address.input]]
     [:input.button.is-primary
      {:type :submit 
       :value "Search"}]]
     
     ))

(def home-controllers

  [{:start (fn [_] (println "Entering newhome page"))
    :stop  (fn [_] (println "Leaving newhome page"))}])