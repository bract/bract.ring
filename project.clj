(defproject bract/bract.ring "0.6.0-SNAPSHOT"
  :description "Bract module for Ring support"
  :url "https://github.com/bract/bract.ring"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :global-vars {*warn-on-reflection* true
                *assert* true
                *unchecked-math* :warn-on-boxed}
  :dependencies [[bract/bract.core "0.6.0-SNAPSHOT"]]
  :profiles {:provided {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :coverage {:plugins [[lein-cloverage "1.0.9"]]}
             :rel {:min-lein-version "2.7.1"
                   :pedantic? :abort}
             :dev {:dependencies [[ring "1.6.3"]
                                  [clj-http "3.7.0"]
                                  [cheshire "5.8.0"]
                                  [ring/ring-jetty-adapter "1.6.3"]
                                  [aleph "0.4.4"]
                                  [http-kit "2.3.0-beta1"]]}
             :c17 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :c18 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :c19 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :dln {:jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
