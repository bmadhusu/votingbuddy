(ns votingbuddy.db.seed
  (:import [org.postgresql.util PSQLException])
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [clojure.edn :as edn]
            ;; [db.core :refer [connection]]
            [java-time :refer [java-date to-sql-date]]
            [votingbuddy.config :refer [env]]))

;; This code borrowed from
;; https://medium.com/@daniel.oliver.king/getting-real-work-done-in-clojure-application-development-683c8129a313

;; NOTES:
;; clojure.java.jdbc has been deprecated; next.jdbc is in vogue now
;; but switching to next.jdbc gave me some grief as I believe the project is using an older version
;; and I think the postgres version in the book is tied to this next.jdbc??!
;; Anyway, I stayed with clojure.java.jdbc
;; However working with dates, I needed to extend the protocol
;; this took a LONG time for me to figure out but eventually the below worked

;; Learnings:
;; 1. An instant in EDN is brought in as a java.util.Date
;; 2. This java.util.Date should be converted to an Instant object
;; 3. A variety of methods can be applied using java-time library

;; Valuable Sites:
;; 1. https://stackoverflow.com/questions/55959630/how-can-one-get-a-java-time-datetime-using-clojure-java-time-from-inst-date-lit
;; 2. https://github.com/dm3/clojure.java-time
;; 3. https://stackoverflow.com/questions/4635680/what-is-the-best-way-to-get-date-and-time-in-clojure
;; 4. https://stackoverflow.com/questions/21242110/convert-java-util-date-to-java-time-localdate
;; 5. https://andersmurphy.com/2019/08/03/clojure-using-java-time-with-jdbc.html
;; 6. https://clojure-doc.org/articles/ecosystem/java_jdbc/using_sql/


(defn inst->sql-timestamp [t]
  (print t)
  (-> t
      (.toInstant)
    ;;   (java-date)
    ;;   (.atZone
    ;;    (java.time.ZoneId/systemDefault))
    ;;   (.toLocalDate)
      (java-time/instant->sql-timestamp)
      )
  )

(extend-protocol jdbc/ISQLValue
  java.time.Instant
  (sql-value [v]
    (inst->sql-timestamp v))
;;   java.time.LocalDate
;;   (sql-value [v]
;;     (Date/valueOf v))
  java.util.Date
  (sql-value [v]
    (inst->sql-timestamp v))

  
  )

(defn insert-seed!
  "Inserts a single seed definition into the database."
  [seed]
  (doseq [{:keys [table data]} seed]
    (jdbc/insert-multi! (env :database-url) table data)))

(defn insert-all-seeds!
  "Reads all files in the seeds directory and inserts their contents into
   the database."
  []
  (->> (.listFiles (io/file (io/resource "seeds")))
       (map slurp)
       (map edn/read-string)
       (map insert-seed!)
       doall))