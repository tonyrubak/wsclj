(ns wsclj.core
  (:require [ring.websocket :as ws]
            [ring.adapter.jetty :as jetty]
            [clojure.string :as string]))

(def users (atom {}))

(def sockets (atom {}))

(def channels (atom []))

(def server (atom nil))

(defn handler [request]
  {::ws/listener
   {:on-open
    (fn [socket]
      (ws/send socket "I will echo your messages"))
    :on-message
    (fn [socket message]
      (cond
        (string/starts-with? message "PING")
        (ws/send socket "PONG")
        (string/starts-with? message "HELLO")
        (let [username (first (rest (string/split message #" ")))]
          (swap! users (fn [users] (assoc users username socket)))
          (swap! sockets (fn [sockets] (assoc sockets socket username)))
          (ws/send socket (string/join " " ["Welcome" username])))
        :else
        (ws/send socket (string/reverse message))))}})

(defn start-jetty! []
  (reset! server
         (jetty/run-jetty #'handler {:port 3000
                                     :join? false})))

