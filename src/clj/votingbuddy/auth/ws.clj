(ns votingbuddy.auth.ws
  (:require [votingbuddy.auth :as auth]))

(defn authorized? [roles-by-id msg]
  (println "in authorized?: " (-> msg
                                  :session
                                  :identity
                                  (auth/identity->roles)))
  (boolean
   (some (roles-by-id (:id msg) #{})
         (-> msg
             :session
             :identity
             (auth/identity->roles)))))

