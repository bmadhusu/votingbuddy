(ns votingbuddy.db.core
  (:require
   [java-time :refer [java-date]]
   [next.jdbc.date-time]
   [next.jdbc.result-set]
   [next.jdbc.prepare]
   [jsonista.core :as json]
   [conman.core :as conman]
   [mount.core :refer [defstate]]
   [votingbuddy.config :refer [env]]
   [hugsql.core]
   [hugsql.adapter]
   [clojure.tools.logging :as log])
  (:import org.postgresql.util.PGobject
           clojure.lang.IPersistentMap
           clojure.lang.IPersistentVector))

(defn read-pg-object [^PGobject obj]
  (cond-> (.getValue obj)
    (#{"json" "jsonb"} (.getType obj))
    (json/read-value json/keyword-keys-object-mapper)))


(defn write-pg-object [v]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (json/write-value-as-string v))))

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

(extend-protocol next.jdbc.prepare/SettableParameter
  IPersistentMap
  (set-parameter [m ^java.sql.PreparedStatement s i]
    (.setObject s i (write-pg-object m))) 
  IPersistentVector
  (set-parameter [v ^java.sql.PreparedStatement s i]
    (.setObject s i (write-pg-object v))))


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
    (.toLocalTime v))
  PGobject
  (read-column-by-label [^PGobject v _]
    (read-pg-object v))
  (read-column-by-index [^PGobject v _2 _3]
    (read-pg-object v)))


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



