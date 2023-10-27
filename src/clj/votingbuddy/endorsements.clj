(ns votingbuddy.endorsements
  (:require
   [votingbuddy.db.core :as db]
   [votingbuddy.validation :refer [validate-endorsement]]))

(defn endorsement-list []
  {:endorsements (vec (db/get-endorsements))})

(defn save-endorsement! [endorsement]
  (println "In save-endorsement! endorsement is " endorsement)
  (if-let [errors (validate-endorsement endorsement)]
    (throw (ex-info "Endorsement is invalid"
                    {:votingbuddy/error-id :validation
                     :errors errors}))
    (do
      (db/save-endorsement! endorsement)
      (println "saved endorsement to DB"))))