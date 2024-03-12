(ns votingbuddy.endorsements
  (:require
   [votingbuddy.db.core :as db]
   [votingbuddy.config :refer [env]]
   [org.httpkit.client :as http]
   [votingbuddy.validation :refer [validate-endorsement]]
   [cheshire.core :refer :all]
   [cheshire.core :as cheshire]))


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

(defn endorsements-by-office [electionID officeList]
  {:endorsements (vec (db/get-endorsements-by-office {:electionID electionID :offices officeList}))})

(defn endorsements-by-address [address]

  (let [api-url "https://content-civicinfo.googleapis.com/civicinfo/v2/representatives"
        api-key (env :civicinfo-key)
        opts {:query-params {:address address
                             :key api-key}}
        {:keys [status headers body error] :as resp}
        @(http/get api-url opts)
        parse-into-json #(parse-string % true)]

    ;;  (-> body
    ;;      (parse-string true)
    ;;      (get :offices)))

    (as-> body b
      (parse-string b true)
      (get b :offices)
      (map #(dissoc % :officialIndices) b)
      (map generate-string b)
      (endorsements-by-office 1 (into-array b))
      (:endorsements b)
      (map #(update % :office_info str)  b)
      (map #(update % :office_info parse-into-json) b)
      (map #(update % :incumbent str) b)
      (map #(update % :incumbent parse-into-json) b))))

    ;; (map #(update % :office_info (comp (partial parse-string true) str)) (:endorsements offices4) )

  ;;  (endorsements-by-office x y)


(comment
  (def resp  (endorsements-by-address "41-57 76 Street, elmhurst ny"))

  (def xxy (generate-string resp))
  (def abc (map generate-string resp))
  (into-array abc)

  (def gg (endorsements-by-office 1  (into-array abc)))

  ;; from: https://stackoverflow.com/questions/45586197/pgobject-type-conversion-in-clojure

  )


