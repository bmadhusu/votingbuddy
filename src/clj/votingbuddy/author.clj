(ns votingbuddy.author
  (:require [votingbuddy.db.core :as db]))

(defn get-author [login]
  (db/get-user* {:login login}))

(defn set-author-profile [login profile]
  (db/set-profile-for-user* {:login login
                             :profile profile}))

