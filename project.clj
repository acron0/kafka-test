(defproject kafka-test "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main kafka-test.core
  ;:repositories [["clojars" "https://clojars-mirror.tcrawley.org/repo/"]]
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.stuartsierra/component "0.3.1"]
                 ;; producer
                 [kafka-clj "3.5.9"]
                 ;; consumer (via ZK)
                 [clj-kafka "0.3.4"]
                 ;; dev
                 [org.clojure/tools.namespace "0.2.3"]
                 [org.clojure/java.classpath "0.2.0"]])
