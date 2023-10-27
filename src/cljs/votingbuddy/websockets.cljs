(ns votingbuddy.websockets
  (:require-macros [mount.core :refer [defstate]])
  (:require [re-frame.core :as rf]
            [taoensso.sente :as sente]
            mount.core))

(defmulti handle-endorsement
  (fn [{:keys [id]} _]
    id))

(defmethod handle-endorsement :endorsement/add
  [_ msg-add-event]
  (rf/dispatch msg-add-event))

(defmethod handle-endorsement :endorsement/creation-errors
  [_ [_ response]]
  (rf/dispatch
   [:form/set-server-errors (:errors response)]))

;; ---------------------
;; Default Handlers

(defmethod handle-endorsement :chsk/handshake
  [{:keys [event]} _]
  (.log js/console "Connection established: " (pr-str event)))

(defmethod handle-endorsement :chsk/state
  [{:keys [event]} _]
  (.log js/console "State Changed: " (pr-str event)))

(defmethod handle-endorsement :default
  [{:keys [event]} _]
  (.warn js/console "Unknown websocket message " (pr-str event)))



(defstate socket
  :start (sente/make-channel-socket!
          "/ws"
          (.-value (.getElementById js/document "token"))
          {:type :auto
           :wrap-recv-evs? false}))

(defn send! [& args]
  (if-let [send-fn (:send-fn @socket)]
    (do
      (apply send-fn args)
      (.log js/console "send endorsement via websocket!"))
    (throw (ex-info "Couldnt send endorsement, channel isn't open!"
                    {:endorsement (first args)}))))

(rf/reg-fx
 :ws/send!
 (fn [{:keys [message timeout callback-event]
       :or {timeout 30000}}]
   (if callback-event
     (send! message timeout #(rf/dispatch (conj callback-event %)))
     (send! message))))

;; ------------
;; Router

(defn receive-endorsement!
  [{:keys [id event] :as ws-message}]
  (do
    (.log js/console "Event received: " (pr-str event))
    (handle-endorsement ws-message event)))

(defstate channel-router
  :start (sente/start-chsk-router!
          (:ch-recv @socket)
          #'receive-endorsement!)
  :stop (when-let [stop-fn @channel-router]
          (stop-fn)))