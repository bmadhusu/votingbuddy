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

(defn endorsements-by-author [electionID author]
  (println "In endorsements-by-author: " author)
  {:endorsements (vec (db/get-endorsements-by-author {:electionID electionID :author author}))})

(defn endorsements-by-organization [orgID]
  {:endorsements (vec (db/get-endorsements-by-organization {:orgID orgID}))})

(defn endorsements-by-candidate [candID]
  {:endorsements (vec (db/get-endorsements-by-candidate {:candID candID}))})

(defn endorsements-for-candidate [candID]
  {:endorsements (vec (db/get-endorsements-for-candidate {:candID candID}))})

(defn endorsements-by-office [electionID officeList]
  (prn "officeList" officeList)
  {:endorsements (vec (db/get-endorsements-by-office {:electionID electionID :offices officeList}))})

;; change this MAGIC # to reflect what election to bring back
;; TODO: this should be looked up based on
;; today's date and next election coming up in 
;; user's vicinity; right now it's hard-coded!
(def election-to-bring-back 2)

(defn endorsements-by-address [address]

  (println "Going to search for address: " address)
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

    (println body)

    (as-> body b
      (parse-string b true)
      (get b :offices)
      (map #(dissoc % :officialIndices) b)
      (map generate-string b)
      (endorsements-by-office election-to-bring-back (into-array b))
      (:endorsements b)
      (map #(update % :office_info str)  b)
      (map #(update % :office_info parse-into-json) b)
      (map #(update % :incumbent str) b)
      (map #(update % :incumbent parse-into-json) b)
      ;; (map #(dissoc % :id :office_info :incumbent) b)
      {:endorsements (vec b)})))

    ;; (map #(update % :office_info (comp (partial parse-string true) str)) (:endorsements offices4) )

  ;;  (endorsements-by-office x y)


(comment
  (def resp  (endorsements-by-address "41-57 76 Street, elmhurst ny"))

  (endorsement-list)
  (def xxy (generate-string resp))
  (def abc (map generate-string resp))
  (into-array abc)

  (def gg (endorsements-by-office 1  (into-array abc)))

  ;; from: https://stackoverflow.com/questions/45586197/pgobject-type-conversion-in-clojure

  (group-by :office_info (:endorsements r))

  (defn dissoc-in
    "Dissociates an entry from a nested associative structure returning a new
    nested structure. keys is a sequence of keys. Any empty maps that result
    will not be present in the new structure."
    [m [k & ks :as keys]]
    (if ks
      (if-let [nextmap (get m k)]
        (let [newmap (dissoc-in nextmap ks)]
          (if (seq newmap)
            (assoc m k newmap)
            (dissoc m k)))
        m)
      (dissoc m k)))
  
  (def rr (->> (:endorsements r)
               (map #(assoc-in % [:office_info :incumbent] (:incumbent %)) )
               (map #(dissoc % :incumbent))
               (group-by :office_info)
               ))

 (into {} (for [[k v] rr] [k (map #(dissoc % :office_info) v)]))

  (def rr (map #(assoc-in % [:office_info :incumbent] (:incumbent %)) (:endorsements r)))
  (map #(dissoc % :incumbent) rr)

  (def r
    {:endorsements
     [{:candidatename "George A. Grasso",
       :id 1815,
       :office_info
       {:name "Queens District Attorney",
        :roles ["governmentOfficer"],
        :levels ["administrativeArea2"],
        :divisionId "ocd-division/country:us/state:ny/county:queens"},
       :incumbent
       {:name "Melinda Katz",
        :urls ["https://queensda.org/" "https://en.wikipedia.org/wiki/Melinda_Katz"],
        :party "Democratic Party",
        :emails ["info@queensda.org"],
        :phones ["(718) 286-6000"],
        :address [{:zip "11415", :city "Queens", :line1 "12501 Queens Boulevard", :state "NY"}],
        :channels [{:id "QueensDAKatz", :type "Facebook"} {:id "QueensDAKatz", :type "Twitter"}]},
       :timestamp #inst "2024-03-09T16:15:06.238-00:00",
       :statement
       "amet, est officia sed magna dolor ullamco eu eiusmod dolore id sint velit deserunt voluptate consectetur proident, cupidatat esse ipsum in ad Duis nulla aliquip nisi irure enim et ut culpa anim Excepteur quis incididunt aliqua. consequat. sunt reprehenderit exercitation",
       :subject "voluptate",
       :orgname "Public Safety Party"}
      {:candidatename "Michael Mossa",
       :id 1815,
       :office_info
       {:name "Queens District Attorney",
        :roles ["governmentOfficer"],
        :levels ["administrativeArea2"],
        :divisionId "ocd-division/country:us/state:ny/county:queens"},
       :incumbent
       {:name "Melinda Katz",
        :urls ["https://queensda.org/" "https://en.wikipedia.org/wiki/Melinda_Katz"],
        :party "Democratic Party",
        :emails ["info@queensda.org"],
        :phones ["(718) 286-6000"],
        :address [{:zip "11415", :city "Queens", :line1 "12501 Queens Boulevard", :state "NY"}],
        :channels [{:id "QueensDAKatz", :type "Facebook"} {:id "QueensDAKatz", :type "Twitter"}]},
       :timestamp #inst "2024-03-09T16:15:06.238-00:00",
       :statement
       "reprehenderit officia dolor eiusmod esse ullamco et mollit veniam, culpa dolore laboris amet, sunt elit, sed magna nostrud consectetur sint proident, occaecat Duis quis velit in pariatur. fugiat do minim commodo Ut dolor irure adipiscing ex in ut laborum. exercitation non ipsum ut aliqua. incididunt deserunt enim voluptate nulla ea anim",
       :subject "sed",
       :orgname "Conservative Party"}
      {:candidatename "Michael Mossa",
       :id 1815,
       :office_info
       {:name "Queens District Attorney",
        :roles ["governmentOfficer"],
        :levels ["administrativeArea2"],
        :divisionId "ocd-division/country:us/state:ny/county:queens"},
       :incumbent
       {:name "Melinda Katz",
        :urls ["https://queensda.org/" "https://en.wikipedia.org/wiki/Melinda_Katz"],
        :party "Democratic Party",
        :emails ["info@queensda.org"],
        :phones ["(718) 286-6000"],
        :address [{:zip "11415", :city "Queens", :line1 "12501 Queens Boulevard", :state "NY"}],
        :channels [{:id "QueensDAKatz", :type "Facebook"} {:id "QueensDAKatz", :type "Twitter"}]},
       :timestamp #inst "2024-03-09T16:15:06.238-00:00",
       :statement
       "dolore in ad labore aliquip ea cupidatat laboris ex et eiusmod ipsum velit aute exercitation in dolor amet, consectetur mollit sint veniam, Excepteur sunt adipiscing minim Lorem non officia id qui dolore fugiat ut est reprehenderit sit consequat. laborum. dolor irure nulla occaecat quis voluptate sed eu magna deserunt enim do nisi pariatur. elit, Ut proident, cillum culpa anim Duis",
       :subject "enim",
       :orgname "Republican Party"}
      {:candidatename "Melinda Katz",
       :id 1815,
       :office_info
       {:name "Queens District Attorney",
        :roles ["governmentOfficer"],
        :levels ["administrativeArea2"],
        :divisionId "ocd-division/country:us/state:ny/county:queens"},
       :incumbent
       {:name "Melinda Katz",
        :urls ["https://queensda.org/" "https://en.wikipedia.org/wiki/Melinda_Katz"],
        :party "Democratic Party",
        :emails ["info@queensda.org"],
        :phones ["(718) 286-6000"],
        :address [{:zip "11415", :city "Queens", :line1 "12501 Queens Boulevard", :state "NY"}],
        :channels [{:id "QueensDAKatz", :type "Facebook"} {:id "QueensDAKatz", :type "Twitter"}]},
       :timestamp #inst "2024-03-09T16:15:06.238-00:00",
       :statement
       "amet, sed ullamco tempor ad dolore sit in Duis nulla velit qui dolore quis laboris labore fugiat Lorem aute est reprehenderit ea incididunt exercitation Excepteur ut sunt culpa nostrud voluptate consectetur adipiscing in eiusmod cillum irure deserunt non officia cupidatat enim magna ipsum minim do pariatur. aliquip consequat.",
       :subject "sit",
       :orgname "Democratic Party"}
      {:candidatename "Fatima Baryab",
       :id 2935,
       :office_info
       {:name "New York City Council Member",
        :roles ["legislatorLowerBody"],
        :levels ["locality"],
        :divisionId "ocd-division/country:us/state:ny/place:new_york/council_district:25"},
       :incumbent
       {:name "Shekar Krishnan",
        :urls ["https://council.nyc.gov/District-25" "https://en.wikipedia.org/wiki/Shekar_Krishnan"],
        :party "Democratic Party",
        :emails ["district25@council.nyc.gov"],
        :phones ["(212) 788-7066"],
        :address [{:zip "10007", :city "New York", :line1 "250 Broadway", :state "NY"}],
        :channels [{:id "voteshekar", :type "Twitter"}]},
       :timestamp #inst "2024-03-09T16:15:06.238-00:00",
       :statement "do minim veniam, voluptate sunt est non",
       :subject "fugiat",
       :orgname "Diversity Party"}
      {:candidatename "Zhile Cao",
       :id 2935,
       :office_info
       {:name "New York City Council Member",
        :roles ["legislatorLowerBody"],
        :levels ["locality"],
        :divisionId "ocd-division/country:us/state:ny/place:new_york/council_district:25"},
       :incumbent
       {:name "Shekar Krishnan",
        :urls ["https://council.nyc.gov/District-25" "https://en.wikipedia.org/wiki/Shekar_Krishnan"],
        :party "Democratic Party",
        :emails ["district25@council.nyc.gov"],
        :phones ["(212) 788-7066"],
        :address [{:zip "10007", :city "New York", :line1 "250 Broadway", :state "NY"}],
        :channels [{:id "voteshekar", :type "Twitter"}]},
       :timestamp #inst "2024-03-09T16:15:06.238-00:00",
       :statement
       "nostrud dolore laboris sit nulla eu reprehenderit exercitation mollit in cillum dolor quis dolore non sint Excepteur consectetur laborum. magna fugiat aliquip consequat. ea proident, ipsum in id in eiusmod voluptate labore dolor velit sunt ex Duis tempor Lorem sed Ut ut est irure culpa amet, cupidatat",
       :subject "aute",
       :orgname "Republican Party"}
      {:candidatename "Zhile Cao",
       :id 2935,
       :office_info
       {:name "New York City Council Member",
        :roles ["legislatorLowerBody"],
        :levels ["locality"],
        :divisionId "ocd-division/country:us/state:ny/place:new_york/council_district:25"},
       :incumbent
       {:name "Shekar Krishnan",
        :urls ["https://council.nyc.gov/District-25" "https://en.wikipedia.org/wiki/Shekar_Krishnan"],
        :party "Democratic Party",
        :emails ["district25@council.nyc.gov"],
        :phones ["(212) 788-7066"],
        :address [{:zip "10007", :city "New York", :line1 "250 Broadway", :state "NY"}],
        :channels [{:id "voteshekar", :type "Twitter"}]},
       :timestamp #inst "2024-03-09T16:15:06.238-00:00",
       :statement
       "officia mollit proident, incididunt dolor Duis exercitation pariatur. non esse Excepteur aliquip et occaecat ullamco velit labore anim magna dolor sit Lorem nisi ut culpa aliqua. enim minim adipiscing ea do sed tempor ad laborum. dolore consequat. id elit, est eiusmod laboris amet, irure dolore ex ut sint deserunt reprehenderit fugiat aute cupidatat ipsum qui voluptate commodo cillum Ut eu veniam, quis sunt nostrud consectetur",
       :subject "non",
       :orgname "Medical Freedom Party"}
      {:candidatename "Shekar Krishnan",
       :id 2935,
       :office_info
       {:name "New York City Council Member",
        :roles ["legislatorLowerBody"],
        :levels ["locality"],
        :divisionId "ocd-division/country:us/state:ny/place:new_york/council_district:25"},
       :incumbent
       {:name "Shekar Krishnan",
        :urls ["https://council.nyc.gov/District-25" "https://en.wikipedia.org/wiki/Shekar_Krishnan"],
        :party "Democratic Party",
        :emails ["district25@council.nyc.gov"],
        :phones ["(212) 788-7066"],
        :address [{:zip "10007", :city "New York", :line1 "250 Broadway", :state "NY"}],
        :channels [{:id "voteshekar", :type "Twitter"}]},
       :timestamp #inst "2024-03-09T16:15:06.238-00:00",
       :statement
       "reprehenderit tempor cupidatat sint enim ea sed in nisi fugiat in adipiscing dolore laborum. nulla sunt cillum commodo elit, mollit anim ex magna veniam, Ut aute ipsum aliqua. minim proident, consectetur ut ullamco qui aliquip consequat. dolore labore velit culpa id laboris ad Excepteur Lorem Duis exercitation",
       :subject "irure",
       :orgname "Working Families Party"}
      {:candidatename "Shekar Krishnan",
       :id 2935,
       :office_info
       {:name "New York City Council Member",
        :roles ["legislatorLowerBody"],
        :levels ["locality"],
        :divisionId "ocd-division/country:us/state:ny/place:new_york/council_district:25"},
       :incumbent
       {:name "Shekar Krishnan",
        :urls ["https://council.nyc.gov/District-25" "https://en.wikipedia.org/wiki/Shekar_Krishnan"],
        :party "Democratic Party",
        :emails ["district25@council.nyc.gov"],
        :phones ["(212) 788-7066"],
        :address [{:zip "10007", :city "New York", :line1 "250 Broadway", :state "NY"}],
        :channels [{:id "voteshekar", :type "Twitter"}]},
       :timestamp #inst "2024-03-09T16:15:06.238-00:00",
       :statement
       "quis occaecat ea sunt ad exercitation deserunt amet, anim consequat. nulla laboris non id aliqua. fugiat culpa cupidatat ex elit, sint labore ipsum dolore et dolore mollit velit consectetur sed ullamco eiusmod commodo enim sit dolor ut magna in Duis incididunt adipiscing cillum in Lorem nostrud eu voluptate aute do pariatur. veniam, officia irure laborum. Ut reprehenderit minim tempor esse in proident, Excepteur qui ut nisi dolor aliquip",
       :subject "ex",
       :orgname "Democratic Party"}]})
  )


