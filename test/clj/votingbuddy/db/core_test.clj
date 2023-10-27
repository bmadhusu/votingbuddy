(ns votingbuddy.db.core-test
  (:require
   [votingbuddy.db.core :refer [*db*] :as db]
   [java-time.pre-java8]
   [luminus-migrations.core :as migrations]
   [clojure.test :refer :all]
   [next.jdbc :as jdbc]
   [votingbuddy.config :refer [env]]
   [mount.core :as mount]))

(use-fixtures
  :once
  (fn [f]
    (mount/start
     #'votingbuddy.config/env
     #'votingbuddy.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(deftest test-candidates
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
    (is (= 1 (db/save-candidate!
              t-conn
              {:name "AOC"
               :statement  "I'm down with the poor and wretched!"
               }
              {})))
    (is (= {:name "AOC"
            :statement  "I'm down with the poor and wretched!"}
           (-> (db/get-candidates t-conn {})
               (first)
               (select-keys [:name :statement]))))))
