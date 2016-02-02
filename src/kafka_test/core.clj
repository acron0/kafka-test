(ns kafka-test.core
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as async :refer [go go-loop chan <! close! put!]]
            [clj-kafka.core :as kafka-core]
            [clj-kafka.new.producer :as kafka]
            [clj-kafka.consumer.zk :as kafka-zk]))

(defn start-queue! [c host port]
  (with-open [p (kafka/producer {"bootstrap.servers" (str host ":" port)}
                                (kafka/byte-array-serializer)
                                (kafka/byte-array-serializer))]
    (go-loop []
      (println "Waiting to produce...")
      (if-let [x (<! c)]
        (do
          (println "Producing =>" x)
          (kafka/send p (kafka/record "test-topic" (.getBytes (str x))))
          (println "Done")
          (recur))))))

(defrecord KafkaProducer [host port]
  component/Lifecycle
  (start [component]
    (println ";; Starting producer..." host port)
    (let [queue-chan (chan 50)]
      (start-queue! queue-chan host port)
      (assoc component :queue queue-chan)))

  (stop [component]
    (println ";; Stopping producer...")
    (close! (:queue component))
    (assoc component :queue nil)))

(defn new-kafka-producer [host port]
  (map->KafkaProducer {:host host :port port}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn do-message!
  [msg]
  (println "Consuming <=" msg))

(defn start-listening! [host port]
  (go
    (let [config {"zookeeper.connect" (str host ":" port)
                  "group.id" "clj-kafka.consumer"
                  "auto.offset.reset" "smallest"
                  "auto.commit.enable" "false"}]
      (kafka-core/with-resource [c (kafka-zk/consumer config)]
        kafka-zk/shutdown
        (let [stream (kafka-zk/create-message-stream c "test-topic")]
          (run! do-message! stream))))))

(defrecord KafkaConsumer [host port]
  component/Lifecycle
  (start [component]
    (println ";; Starting consumer..." host port)
    (start-listening! host port)
    component)

  (stop [component]
    (println ";; Stopping consumer...")))

(defn new-kafka-consumer [host port]
  (map->KafkaConsumer {:host host :port port}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn kafka-test-system
  [{:keys [host kafka-port zookeeper-port]}]
  (component/system-map
   :producer (new-kafka-producer host kafka-port)
   :consumer (new-kafka-consumer host zookeeper-port)))

(def system (kafka-test-system
             {:host "127.0.0.1"
              :kafka-port 9092
              :zookeeper-port 2181}))

(defn -main
  "I don't do a whole lot."
  []
  (println "Hello, World!"))
