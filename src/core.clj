(ns wsclj.core
  (:require [ring.websocket :as ws]
            [ring.adapter.jetty :as jetty]
            [clojure.string :as string]))

(def heartbeatInterval 25000)

(def users (atom {}))

(def sockets (atom {}))

(def channels (atom []))

(def server (atom nil))

(defmulti dispatch (fn [msg _] (first (string/split msg #" "))))

(defmethod dispatch "PING" [_ socket]
  (ws/send socket "PONG"))

(defmethod dispatch "HELLO" [msg socket]
  (let [split (string/split msg #" ")
        username (string/join " " (rest split))]
    (swap! users (fn [users] (assoc users username socket)))
    (swap! sockets (fn [sockets] (assoc sockets socket username)))
    (ws/send socket (str "HEARTBEAT " heartbeatInterval))))

(defmethod dispatch "MESSAGE" [msg socket]
  (let [message (string/join " " (rest (string/split msg #" ")))
        sender (@sockets socket)]
    (doseq [dest (vals @users)]
      (ws/send dest (string/join " " [(str "<" sender ">") message])))))

(defn handler [request]
  {::ws/listener
   {:on-message
    (fn [socket message]
      (dispatch message socket))}})

(defn start-jetty! []
  (reset! server
         (jetty/run-jetty #'handler {:port 3000
                                     :join? false})))

