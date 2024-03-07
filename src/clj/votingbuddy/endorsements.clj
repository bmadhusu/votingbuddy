(ns votingbuddy.endorsements
  (:require
   [votingbuddy.db.core :as db]
   [votingbuddy.config :refer [env]]
   [org.httpkit.client :as http]
   [votingbuddy.validation :refer [validate-endorsement]]))


(defn endorsement-list []
  {:endorsements (vec (db/get-endorsements))})

(defn save-endorsement! [{:keys [login]} endorsement]
  (println "In save-endorsement! endorsement is " endorsement)
  (println "Login is: " login)
  (if-let [errors (validate-endorsement endorsement)]
    (throw (ex-info "Endorsement is invalid"
                    {:votingbuddy/error-id :validation
                     :errors errors}))
    (do
      (println "trying to save endorsement to DB")
      ;; (forcandidateid, linkedid, endorsertype, sourceuserid, is_active, is_visible, subject, statement)
      (let [new-endorsement (assoc endorsement :sourceuserid login :electionid 1 :forcandidateid 124 :linkedid 5 :endorsertype "organization" :is_active true :is_visible true)]
        (db/save-endorsement! new-endorsement))

      (println "saved endorsement to DB"))))

(defn endorsements-by-author [author]
  (println "In endorsements-by-author: " author)
  {:endorsements (vec (db/get-endorsements-by-author {:author author}))})

(defn endorsements-by-organization [orgID]
  {:endorsements (vec (db/get-endorsements-by-organization {:orgID orgID}))})

(defn endorsements-by-candidate [candID]
  {:endorsements (vec (db/get-endorsements-by-candidate {:candID candID}))})

(defn endorsements-for-candidate [candID]
  {:endorsements (vec (db/get-endorsements-for-candidate {:candID candID}))})

(defn endorsements-by-ocdid [electionID ocdList]
  {:endorsements (vec (db/get-endorsements-by-ocd {:electionID electionID :ocds ocdList}))})

(defn endorsements-by-address [address]
  (let [api-url "https://content-civicinfo.googleapis.com/civicinfo/v2/representatives"
        api-key (env :civicinfo-key)
        opts {:query-params {:address address
                             :key api-key}}
        {:keys [status headers body error] :as resp}
        @(http/get api-url opts)]

    (print resp)
    resp
  ;;  (endorsements-by-ocdid x y)
    ))



