;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.wrapper-test
  (:require
    [clojure.test :refer :all]
    [clojure.edn         :as edn]
    [cheshire.core       :as cheshire]
    [clj-http.client     :as client]
    [ring.adapter.jetty  :as jetty]
    [bract.ring.keydef   :as ring-kdef]
    [bract.ring.wrapper  :as wrapper])
  (:import
    [org.eclipse.jetty.server Server]))


(def default-response {:status 200
                       :body "default"
                       :headers {"Content-Type" "text/plain"}})


(defmacro with-jetty
  [handler options & body]
  `(let [^Server server# (jetty/run-jetty ~handler ~options)]
     (try
       ~@body
       (finally
         (try
           (.stop server#)
           (catch Exception e#
             (.printStackTrace e#)))))))


(defn roundtrip-get
  [handler jetty-options path]
  (with-jetty handler (conj {:port 3000 :join? false}
                        jetty-options)
    (-> (client/get (str "http://localhost:3000" path))
      (dissoc 
        :trace-redirects :length :orig-content-encoding
        :request-time :repeatable? :protocol-version
        :streaming? :chunked? :reason-phrase)
      (update-in [:headers] dissoc "Date" "Connection" "Server"))))


(defn handler
  ([_] default-response)
  ([_ respond _] (respond default-response)))


(deftest test-info-edn
  (let [wrapped-handler (wrapper/info-edn-wrapper handler {})
        body-map (fn [response] (-> response
                                  :body
                                  edn/read-string
                                  (select-keys [:os-name :os-arch :os-memory-physical-total])))]
    (is (= default-response
          (wrapped-handler {:uri "/foo"})
          (roundtrip-get wrapped-handler {} "/foo")
          (roundtrip-get wrapped-handler {:async? true} "/foo")))
    (testing "info URI without trailing slash"
      (is (not= default-response
            (wrapped-handler {:uri "/info/edn"})))
      (is (= (-> (wrapped-handler {:uri "/info/edn"})
               body-map)
            (-> (roundtrip-get wrapped-handler {} "/info/edn")
              body-map)
            (-> (roundtrip-get wrapped-handler {:async? true} "/info/edn")
              body-map))))
    (testing "info URI with trailing slash"
      (is (not= default-response
            (wrapped-handler {:uri "/info/edn/"})))
      (is (= (-> (wrapped-handler {:uri "/info/edn/"})
               body-map)
            (-> (roundtrip-get wrapped-handler {} "/info/edn/")
              body-map)
            (-> (roundtrip-get wrapped-handler {:async? true} "/info/edn/")
              body-map))))))


(deftest test-info-json
  (let [wrapped-handler (wrapper/info-json-wrapper handler {} cheshire/generate-string)
        body-map (fn [response] (-> response
                                  :body
                                  cheshire/parse-string
                                  (select-keys [:os-name :os-arch :os-memory-physical-total])))]
    (is (= default-response
          (wrapped-handler {:uri "/foo"})
          (roundtrip-get wrapped-handler {} "/foo")
          (roundtrip-get wrapped-handler {:async? true} "/foo")))
    (testing "info URI without trailing slash"
      (is (not= default-response
            (wrapped-handler {:uri "/info/json"})))
      (is (= (-> (wrapped-handler {:uri "/info/json"})
               body-map)
            (-> (roundtrip-get wrapped-handler {} "/info/json")
              body-map)
            (-> (roundtrip-get wrapped-handler {:async? true} "/info/json")
              body-map))))
    (testing "info URI with trailing slash"
      (is (not= default-response
            (wrapped-handler {:uri "/info/json/"})))
      (is (= (-> (wrapped-handler {:uri "/info/json/"})
               body-map)
            (-> (roundtrip-get wrapped-handler {} "/info/json/")
              body-map)
            (-> (roundtrip-get wrapped-handler {:async? true} "/info/json/")
              body-map))))))


(deftest test-ping
  (let [wrapped-handler (wrapper/ping-wrapper handler {})]
    (is (= default-response
          (wrapped-handler {:uri "/foo"})
          (roundtrip-get wrapped-handler {} "/foo")
          (roundtrip-get wrapped-handler {:async? true} "/foo")))
    (testing "ping URI without trailing slash"
      (is (not= default-response
            (wrapped-handler {:uri "/ping"})))
      (is (= "pong"
            (-> (wrapped-handler {:uri "/ping"})
              :body)
            (-> (roundtrip-get wrapped-handler {} "/ping")
              :body)
            (-> (roundtrip-get wrapped-handler {:async? true} "/ping")
              :body))))
    (testing "ping URI with trailing slash"
      (is (not= default-response
            (wrapped-handler {:uri "/ping/"})))
      (is (= "pong"
            (-> (wrapped-handler {:uri "/ping/"})
              :body)
            (-> (roundtrip-get wrapped-handler {} "/ping/")
              :body)
            (-> (roundtrip-get wrapped-handler {:async? true} "/ping/")
              :body))))))


(deftest test-uri-trailing-space
  (let [handler (fn
                  ([request] {:status 200
                              :body (:uri request)})
                  ([request respond raise] (respond {:status 200
                                                     :body (:uri request)})))
        add-trs (wrapper/request-update-wrapper handler {} wrapper/add-uri-trailing-slash)
        rem-trs (wrapper/request-update-wrapper handler {} wrapper/remove-uri-trailing-slash)]
    (testing "add trailing slash"
      (is (= {:status 200
              :body "/foo/"}
            (add-trs {:uri "/foo"})
            (dissoc (roundtrip-get add-trs {} "/foo") :headers)
            (dissoc (roundtrip-get add-trs {:async? true} "/foo") :headers)))
      (is (= {:status 200
              :body "/foo/"}
            (add-trs {:uri "/foo/"})
            (dissoc (roundtrip-get add-trs {} "/foo/") :headers)
            (dissoc (roundtrip-get add-trs {:async? true} "/foo/") :headers))))
    (testing "remove trailing slash"
      (is (= {:status 200
              :body "/foo"}
            (rem-trs {:uri "/foo"})
            (dissoc (roundtrip-get rem-trs {} "/foo") :headers)
            (dissoc (roundtrip-get rem-trs {:async? true} "/foo") :headers)))
      (is (= {:status 200
              :body "/foo"}
            (rem-trs {:uri "/foo/"})
            (dissoc (roundtrip-get rem-trs {} "/foo/") :headers)
            (dissoc (roundtrip-get rem-trs {:async? true} "/foo/") :headers))))))


(deftest test-uri-prefix-match
  (testing "matcher"
    (doseq [[matcher
             response] [[(wrapper/make-uri-prefix-matcher "/prefix" true :backup)  {:uri "/foo"
                                                                                    :backup "/prefix/foo"}]
                        [(wrapper/make-uri-prefix-matcher "/prefix" false :backup) {:uri "/prefix/foo"
                                                                                    :backup "/prefix/foo"}]
                        [(wrapper/make-uri-prefix-matcher "/prefix" false nil)     {:uri "/prefix/foo"}]]]
      (is (= response
            (matcher {:uri "/prefix/foo"})))
      (is (= nil
            (matcher {:uri "/foo/bar"})))
      (is (= nil
            (matcher {:uri "/prefix"})))))
  (testing "wrapper"
    (let [wrapped (wrapper/uri-prefix-match-wrapper handler {} "/prefix" true :backup)]
      (is (= {:status 200
              :body "default"
              :headers {"Content-Type" "text/plain"}}
            (wrapped {:uri "/prefix/foo"}))))))
