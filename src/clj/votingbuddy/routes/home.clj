(ns votingbuddy.routes.home
  (:require
   [votingbuddy.layout :as layout]
   [votingbuddy.middleware :as middleware]))


(defn home-page [request]
  (layout/render request "home.html"))

(defn about-page [request]
  (layout/render request "about.html"))


(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/about" {:get about-page}]])

