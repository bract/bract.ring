(defproject bract/bract.ring "0.6.2-0.2.0"
  :description "Bract module for Ring support"
  :url "https://github.com/bract/bract.ring"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :global-vars {*warn-on-reflection* true
                *assert* true
                *unchecked-math* :warn-on-boxed}
  :pedantic? :warn
  :dependencies [[bract/bract.core "0.6.2"]]
  :profiles {:provided {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :coverage {:plugins [[lein-cloverage "1.0.9"]]}
             :rel {:min-lein-version "2.7.1"
                   :pedantic? :abort}
             :dev {:dependencies [[ring "1.6.3"]
                                  [clj-http "3.7.0" :exclusions [riddley]]
                                  [cheshire "5.10.0"]
                                  ;; web servers
                                  [aleph                   "0.4.4"]
                                  [http-kit                "2.5.3"]
                                  [org.immutant/immutant   "2.1.10"]
                                  [ring/ring-jetty-adapter "1.6.3"]
                                  [nginx-clojure/nginx-clojure-embed "0.5.1"]]}
             :c07 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :c08 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :c09 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :c10 {:dependencies [[org.clojure/clojure "1.10.3-rc1"]]}
             :dln {:jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}
  :aliases {"test-all" ["with-profile" "c07,dev:c08,dev:c09,dev:c10,dev" "test"]})
