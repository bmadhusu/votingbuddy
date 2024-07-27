(ns votingbuddy.routes.app
  (:require
   #?@(:clj [[votingbuddy.layout :as layout]
             [votingbuddy.middleware :as middleware]]
       :cljs [[votingbuddy.views.home :as home]
              [votingbuddy.views.author :as author]
              [votingbuddy.views.newhome :as newhome]])))

#?(:clj
   (defn home-page [request]
     (layout/render
      request
      "home.html")))

(defn app-routes []
  [""
   #?(:clj {:middleware [middleware/wrap-csrf]
            :get home-page})
   ["/"
    (merge
     {:name ::home}
     #?(:cljs
        {:controllers home/home-controllers
         :view #'home/home}))]
   ["/newhome"
    (merge
     {:name ::newhome}
     #?(:cljs
        {:controllers newhome/home-controllers
         :view #'newhome/home}))]
   ["/user/:user"
    (merge
     {:name ::author}
     #?(:cljs {:controllers author/author-controllers
               :view #'author/author}))]])

