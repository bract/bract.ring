(defproject bract/bract.ring "0.6.0-alpha1"
  :description "Bract module for Ring support"
  :url "https://github.com/bract/bract.ring"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :global-vars {*warn-on-reflection* true
                *assert* true
                *unchecked-math* :warn-on-boxed}
  :pedantic? :warn
  :dependencies [[bract/bract.core "0.6.0-alpha1"]]
  :profiles {:provided {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :coverage {:plugins [[lein-cloverage "1.0.9"]]}
             :rel {:min-lein-version "2.7.1"
                   :pedantic? :abort}
             :dev {:dependencies [[ring "1.6.3"]
                                  [clj-http "3.7.0" :exclusions [riddley]]
                                  [cheshire "5.8.0"]
                                  ;; web servers
                                  [aleph                   "0.4.4"]
                                  [http-kit                "2.3.0-beta1"]
                                  [org.immutant/immutant   "2.1.10"]
                                  [ring/ring-jetty-adapter "1.6.3"]]}
             :c17 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :c18 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :c19 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :dln {:jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
