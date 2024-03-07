(ns votingbuddy.voterinfo
  (:require [org.httpkit.client :as http]))


(def api-url "https://content-civicinfo.googleapis.com/civicinfo/v2/representatives")

;;   ?address=4157%2076%20Street%20Elmhurst%20ny%2011373&
              
;;               key=AIzaSyAa8yy0GdcGPHdtD083HiGGx_S0vMPScDM")

(def api-key "AIzaSyA721yY4-WGxI-Pop55jbn6RqZ-ZluaTu8")

(def opts {:query-params {:address (http/url-encode "41-57 76 Street elmhurst ny 11373")
                          :key api-key}})

;; (http/url-encode (str "/country:us/state:" st))

(let [{:keys [status headers body error] :as resp} 
      @(http/get api-url opts)]
  (print resp))