(ns votingbuddy.routes.websockets
  (:require
   [clojure.tools.logging :as log]
   [votingbuddy.middleware :as middleware]
   [mount.core :refer [defstate]]
   [votingbuddy.endorsements :as endorse]
   [taoensso.sente :as sente]
   [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
   [votingbuddy.session :as session]
   [votingbuddy.auth :as auth]
   [votingbuddy.auth.ws :refer [authorized?]]))


(defstate socket
  :start (sente/make-channel-socket!
          (get-sch-adapter)
          {:user-id-fn (fn [ring-req]
                         (get-in ring-req [:params :client-id]))}))

(defn send! [uid endorsement]
  (println "Sending endorsement: " endorsement)
  ((:send-fn socket) uid endorsement))

(defmulti handle-endorsement (fn [{:keys [id]}]
                               id))

(defmethod handle-endorsement :default
  [{:keys [id]}]
  (log/debug "Received unrecognized websocket event type: " id)
  {:error (str "Unrecognized websocket event type: " (pr-str id))
   :id    id})

(defmethod handle-endorsement :endorsement/create!
  [{:keys [?data uid session] :as endorsement}]
  (let [response (try
                   (log/debug "In websocket code!!")
                   (endorse/save-endorsement! (:identity session) ?data)
                   (assoc ?data :timestamp (java.util.Date.))
                   (catch Exception e
                     (let [{id         :votingbuddy/error-id
                            errors     :errors} (ex-data e)]
                       (case id
                         :validation
                         {:errors errors}
                         ;;else
                         {:errors
                          {:server-error ["Failed to save endorsement!!"]}}))))]
    (if (:errors response)
      (do
        (log/debug "Failed to save endorsement: " ?data)
        response)
      (do
        (doseq [uid (:any @(:connected-uids socket))]
          (send! uid [:endorsement/add response]))
        {:success true}))))

(defn receive-endorsement! [{:keys [id ?reply-fn ring-req]
                             :as endorsement}]
  (case id
    :chsk/bad-package     (log/debug "Bad Package:\n" endorsement)
    :chsk/badevent       (log/debug "Bad Event:\n" endorsement)
    :chsk/uidport-open    (log/trace (:event endorsement))
    :chsk/uidport-close   (log/trace (:event endorsement))
    :chsk/ws-ping         nil
    ;;ELSE
    (let [reply-fn (or ?reply-fn (fn [_]))
          session (session/read-session ring-req)
          endorsement (-> endorsement
                          (assoc :session session))]
      (log/debug "Got endorsement with id: " id)
      (log/debug "endorsement is " endorsement)
      (if (authorized? auth/roles endorsement)
        (when-some [response (handle-endorsement endorsement)]
          (reply-fn response))
        (do
          (log/info "Unauthorized message: " id)
          (reply-fn {:message "You are not authorized to perform this action!"
                     :errors {:unauthorized true}}))))))

;; (defonce channels (atom #{}))

;; (defn connect! [channel]
;;   (log/info "Channel opened")
;;   (swap! channels conj channel))

;; (defn disconnect! [channel status]
;;   (log/info "Channel closed" status)
;;   (swap! channels disj channel))


;; (defn handler [request]
;;   (http-kit/with-channel request channel
;;     (connect! channel)
;;     (http-kit/on-close channel (partial disconnect! channel))
;;     (http-kit/on-receive channel (partial handle-endorsement! channel))))

  (defstate channel-router
    :start (sente/start-chsk-router!
            (:ch-recv socket)
            #'receive-endorsement!)
    :stop (when-let [stop-fn channel-router]
            (stop-fn)))

  (defn websocket-routes []
    ["/ws"
     {:middleware [middleware/wrap-csrf
                   middleware/wrap-formats]
      :get (:ajax-get-or-ws-handshake-fn socket)
      :post (:ajax-post-fn socket)}])

