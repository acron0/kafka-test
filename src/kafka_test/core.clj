(ns kafka-test.core
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as async :refer [ chan <! close! put!]]
            ;; producer
            [kafka-clj.client :as kafka]
            ;; consumer
            [clj-kafka.core :as kafka-core]
            [clj-kafka.consumer.zk :as kafka-zk]
            ;; dev
            [clojure.tools.namespace.repl :refer (refresh)]))

(defn start-queue! [c host port topic]
  (let [conn (kafka/create-connector [{:host "localhost" :port 9092}]
                                     {:flush-on-write true})]
    (async/go-loop []
      (if-let [x (<! c)]
        (do
          (println "Producing =>" x)
          (kafka/send-msg conn topic (.getBytes x))
          (recur))))))

(defrecord KafkaProducer [host port topic]
  component/Lifecycle
  (start [component]
    (println ";; Starting producer..." host port)
    (let [queue-chan (chan 50)]
      (start-queue! queue-chan host port topic)
      (assoc component :queue queue-chan)))

  (stop [component]
    (println ";; Stopping producer..." (:queue component))
    (if-let [c (:queue component)]
      (close! c))
    (assoc component :queue nil)))

(defn new-kafka-producer [host port topic]
  (map->KafkaProducer {:host host :port port :topic topic}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn do-message!
  [msg]
  (println "Consuming <=" (-> msg
                              kafka-core/to-clojure
                              :value
                              (String. "UTF-8"))))

(defn start-listening! [host port topic]
  (async/go
    (let [config {"zookeeper.connect" (str host ":" port)
                  "group.id" "clj-kafka.consumer"
                  "auto.offset.reset" "smallest"
                  "auto.commit.enable" "true"}]
      (kafka-core/with-resource [c (kafka-zk/consumer config)]
        kafka-zk/shutdown
        (let [stream (kafka-zk/create-message-stream c topic)]
          (run! do-message! stream))))))

(defrecord KafkaConsumer [host port topic]
  component/Lifecycle
  (start [component]
    (println ";; Starting consumer..." host port)
    (start-listening! host port topic)
    component)

  (stop [component]
    (println ";; Stopping consumer...")
    component))

(defn new-kafka-consumer [host port topic]
  (map->KafkaConsumer {:host host :port port :topic topic}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn kafka-test-system
  [{:keys [host kafka-port zookeeper-port topic]}]
  (component/system-map
   :producer (new-kafka-producer host kafka-port topic)
   :consumer (new-kafka-consumer host zookeeper-port topic)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def system nil)

(defn init []
  (alter-var-root #'system
                  (constantly (kafka-test-system
                               {:host "127.0.0.1"
                                :kafka-port 9092
                                :zookeeper-port 2181
                                :topic "test"}))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system
                  (fn [s] (when s (component/stop s)))))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'kafka-test.core/go))

(defn -main
  "I don't do a whole lot."
  []
  (go))

(defn send!
  [msg]
  (put! (-> system :producer :queue) msg))
