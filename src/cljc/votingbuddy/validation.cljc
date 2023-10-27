(ns votingbuddy.validation
  (:require
   [struct.core :as st]))

(def message-schema
  [[:subject
    st/required
    st/string]
   [:statement
    st/required
    st/string
    {:message "message must contain at least 10 characters"
     :validate (fn [msg] (>= (count msg) 10))}]])


(defn validate-endorsement [params]
  (first (st/validate params message-schema)))