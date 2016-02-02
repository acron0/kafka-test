(ns kafka-test.core
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as async :refer :all]))

(defn start-queue! [queue-chan]
  (go-loop []
    (let [x (<! c)]
      (println "Got a value in this loop:" x))
    (recur)))

(defrecord KafkaProducer [host port connection]
  component/Lifecycle
  (start [component]
    (println ";; Starting producer...")
    (let [queue-chan (chan 50)]
      (start-queue! queue-chan)
      (assoc component :queue queue-chan)))

  (stop [component]
    (println ";; Stopping database")
    ;; In the 'stop' method, shut down the running
    ;; component and release any external resources it has
    ;; acquired.
    (.close connection)
    ;; Return the component, optionally modified. Remember that if you
    ;; dissoc one of a record's base fields, you get a plain map.
    (assoc component :connection nil)))

(defn -main
  "I don't do a whole lot."
  []
  (println "Hello, World!"))
