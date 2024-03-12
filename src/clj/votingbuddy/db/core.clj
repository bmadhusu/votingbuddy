(ns votingbuddy.db.core
  (:require
   [java-time :refer [java-date]]
   [next.jdbc.date-time]
   [next.jdbc.result-set]
   [conman.core :as conman]
   [mount.core :refer [defstate]]
   [votingbuddy.config :refer [env]]
   [hugsql.core]
   [hugsql.adapter]
   [clojure.tools.logging :as log]))

(defstate ^:dynamic *db*
  :start (conman/connect! {:jdbc-url (env :database-url)})
  :stop (conman/disconnect! *db*))

(conman/bind-connection *db* "sql/queries.sql")
(hugsql.core/def-sqlvec-fns "sql/queries.sql")

(defn sql-timestamp->inst [t]
  (-> t
      (.toLocalDateTime)
      (.atZone
       (java.time.ZoneId/systemDefault))
      (java-date)))

(extend-protocol next.jdbc.result-set/ReadableColumn
  java.sql.Timestamp
  (read-column-by-label [^java.sql.Timestamp v _]
    (sql-timestamp->inst v))
  (read-column-by-index [^java.sql.Timestamp v _2 _3]
    (sql-timestamp->inst v))
  java.sql.Date
  (read-column-by-label [^java.sql.Date v _]
    (.toLocalDate v))
  (read-column-by-index [^java.sql.Date v _2 _3]
    (.toLocalDate v))
  java.sql.Time
  (read-column-by-label [^java.sql.Time v _]
    (.toLocalTime v))
  (read-column-by-index [^java.sql.Time v _2 _3]
    (.toLocalTime v)))


;; below code from
;; https://stackoverflow.com/questions/47035273/log-sql-statments-queries-executed-by-hugsql
;; need to [require clojure.tools.logging :as log]

(defn log-sqlvec [sqlvec]
  (log/info (->> sqlvec
                 (map #(clojure.string/replace (or % "") #"\n" ""))
                 (clojure.string/join " ; "))))

(defn log-command-fn [this db sqlvec options]
  (log-sqlvec sqlvec)
  (condp contains? (:command options)
    #{:!} (hugsql.adapter/execute this db sqlvec options)
    #{:? :<!} (hugsql.adapter/query this db sqlvec options)))

(defmethod hugsql.core/hugsql-command-fn :! [sym] `log-command-fn)
(defmethod hugsql.core/hugsql-command-fn :<! [sym] `log-command-fn)
(defmethod hugsql.core/hugsql-command-fn :? [sym] `log-command-fn)
