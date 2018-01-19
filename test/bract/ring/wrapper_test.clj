;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.wrapper-test
  (:require
    [clojure.test :refer    :all]
    [clojure.edn            :as edn]
    [cheshire.core          :as cheshire]
    [clj-http.client        :as client]
    [ring.middleware.params :as rmp]
    [ring.adapter.jetty     :as jetty]
    [bract.core.keydef      :as core-kdef]
    [bract.ring.keydef      :as ring-kdef]
    [bract.ring.wrapper     :as wrapper])
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
    (-> (str "http://localhost:3000" path)
      (client/get {:throw-exceptions false})
      (dissoc
        :trace-redirects :length :orig-content-encoding
        :request-time :repeatable? :protocol-version
        :streaming? :chunked? :reason-phrase)
      (update-in [:headers] dissoc "Date" "Connection" "Server"))))


(defn roundtrip-put
  ([handler jetty-options path]
    (roundtrip-put handler jetty-options path nil))
  ([handler jetty-options path body]
    (with-jetty handler (conj {:port 3000 :join? false}
                          jetty-options)
      (let [assoc-some (fn [m k v] (if (some? v)
                                     (assoc m k v)
                                     m))]
        (-> (str "http://localhost:3000" path)
          (client/put (assoc-some {:throw-exceptions false} :body body))
          (dissoc
            :trace-redirects :length :orig-content-encoding
            :request-time :repeatable? :protocol-version
            :streaming? :chunked? :reason-phrase)
          (update-in [:headers] dissoc "Date" "Connection" "Server"))))))


(defn handler
  ([_] default-response)
  ([_ respond _] (respond default-response)))


(deftest test-health-check
  (doseq [{:keys [health-uri
                  content-type
                  body-encoder]} [{:health-uri "/health"
                                   :content-type "application/edn"
                                   :body-encoder pr-str}
                                  {:health-uri "/health"
                                   :content-type "application/json"
                                   :body-encoder cheshire/generate-string}]]
    (testing "sanity checks"
      (let [wrapped (-> handler
                      (wrapper/health-check-wrapper {:bract.core/config {}}
                        {:body-encoder body-encoder
                         :content-type content-type}))]
        (is (= {:status 200
                :body "default"
                :headers {"Content-Type" "text/plain"}}
              (roundtrip-get wrapped {} "/")
              (roundtrip-get wrapped {:async true} "/")) "no health check - regular URI cannot trigger health check")
        (is (= {:status 405
                :headers {"Content-Type" "text/plain"}
                :body "Expected HTTP GET request for health check endpoint, but found PUT"}
              (roundtrip-put wrapped {} health-uri)
              (roundtrip-put wrapped {:async true} health-uri)) "health check - PUT request should return HTTP 405")
        (is (= {:status 200
                :headers {"Content-Type" content-type}
                :body (body-encoder {:status :healthy, :components []})}
              (roundtrip-get wrapped {} health-uri)
              (roundtrip-get wrapped {:async true} health-uri)) "health check - no component")))
    (testing "components"
      (let [mysql-broken {:id     :mysql
                          :status :critical
                          :impact :hard
                          :breaker :half-open
                          :retry-in "14000ms"}
            mysql-status {:id     :mysql
                          :status :degraded
                          :impact :hard
                          :breaker :half-open
                          :retry-in "14000ms"}
            cache-status {:id     :cache
                          :status :critical
                          :impact :soft}
            disk-status  {:id     :disk
                          :status :healthy
                          :impact :none
                          :free-gb 39.42}
            critical-hnd (-> handler
                           (wrapper/health-check-wrapper {:bract.core/config {}
                                                          :bract.core/health-check [(fn [] mysql-broken)]}
                             {:body-encoder body-encoder
                              :content-type content-type}))
            degraded-hnd (-> handler
                           (wrapper/health-check-wrapper {:bract.core/config {}
                                                          :bract.core/health-check [(fn [] mysql-status)
                                                                                    (fn [] cache-status)
                                                                                    (fn [] disk-status)]}
                             {:body-encoder body-encoder
                              :content-type content-type}))
            healthy-hnd  (-> handler
                           (wrapper/health-check-wrapper {:bract.core/config {}
                                                          :bract.core/health-check [(fn [] disk-status)]}
                             {:body-encoder body-encoder
                              :content-type content-type}))]
        (is (= {:status 503
                :headers {"Content-Type" content-type}
                :body (body-encoder {:status :critical
                                     :components [mysql-broken]})}
              (roundtrip-get critical-hnd {} health-uri)
              (roundtrip-get critical-hnd {:async true} health-uri)) "health check - critical status")
        (is (= {:status 500
                :headers {"Content-Type" content-type}
                :body (body-encoder {:status :degraded
                                     :components [mysql-status
                                                  cache-status
                                                  disk-status]})}
              (roundtrip-get degraded-hnd {} health-uri)
              (roundtrip-get degraded-hnd {:async true} health-uri)) "health check - degraded status")
        (is (= {:status 200
                :headers {"Content-Type" content-type}
                :body (body-encoder {:status :healthy
                                     :components [disk-status]})}
              (roundtrip-get healthy-hnd {} health-uri)
              (roundtrip-get healthy-hnd {:async true} health-uri)) "health check - healthy status")))))


(deftest test-info-endpoint
  (doseq [{:keys [body-encoder
                  body-decoder
                  content-type]} [{:body-encoder pr-str
                                   :body-decoder edn/read-string
                                   :content-type "application/edn"}
                                  {:body-encoder cheshire/generate-string
                                   :body-decoder cheshire/parse-string
                                   :content-type "application/json"}]]
    (let [wrapped-handler (wrapper/info-endpoint-wrapper handler {:bract.core/config {}}
                            {:body-encoder body-encoder
                             :content-type content-type})
          wrapped-custom  (wrapper/info-endpoint-wrapper handler {:bract.core/config {}
                                                                  :bract.core/runtime-info [#(do {:foo 10
                                                                                                  :bar 20})]}
                            {:body-encoder body-encoder
                             :content-type content-type})
          body-map (fn [response] (-> response
                                    :body
                                    body-decoder
                                    (select-keys [:os-name :os-arch :os-memory-physical-total])))]
      (is (= default-response
            (wrapped-handler {:uri "/foo"})
            (roundtrip-get wrapped-handler {} "/foo")
            (roundtrip-get wrapped-handler {:async? true} "/foo")))
      (testing "info URI without trailing slash"
        (is (not= default-response
              (wrapped-handler {:uri "/info" :request-method :get})))
        (is (= (-> (wrapped-handler {:uri "/info/" :request-method :get})
                 body-map)
              (-> (roundtrip-get wrapped-handler {} "/info")
                body-map)
              (-> (roundtrip-get wrapped-handler {:async? true} "/info")
                body-map))))
      (testing "info URI with trailing slash"
        (is (not= default-response
              (wrapped-handler {:uri "/info/" :request-method :get})))
        (is (= (-> (wrapped-handler {:uri "/info/" :request-method :get})
                 body-map)
              (-> (roundtrip-get wrapped-handler {} "/info/")
                body-map)
              (-> (roundtrip-get wrapped-handler {:async? true} "/info/")
                body-map))))
      (testing "info URI with custom info generators"
        (is (= (-> (wrapped-custom {:uri "/info/" :request-method :get})
                 body-map)
              (-> (roundtrip-get wrapped-custom {} "/info/")
                body-map)
              (-> (roundtrip-get wrapped-custom {:async? true} "/info/")
                body-map)))))))


(deftest test-ping
  (let [wrapped-handler (wrapper/ping-endpoint-wrapper handler {:bract.core/config {}})]
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
        add-trs (wrapper/request-update-wrapper handler {:bract.core/config {}} wrapper/add-uri-trailing-slash)
        rem-trs (wrapper/request-update-wrapper handler {:bract.core/config {}} wrapper/remove-uri-trailing-slash)]
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
    (let [wrapped (wrapper/uri-prefix-match-wrapper handler {:bract.core/config {}} "/prefix" true :backup)]
      (is (= {:status 200
              :body "default"
              :headers {"Content-Type" "text/plain"}}
            (wrapped {:uri "/prefix/foo"}))))))


(deftest test-params-normalize
  (let [handler (fn params-as-body
                  ([request] {:status 200
                              :body (str (:params request))})
                  ([request respond raise] (respond (params-as-body request))))
        wrapped (-> handler
                  (wrapper/params-normalize-wrapper {:bract.core/config {}} clojure.core/identity)
                  rmp/wrap-params)]
    (is (= {:headers {}
            :status 200
            :body "{\"bar\" [\"20\"]}"}
          (roundtrip-get wrapped {} "/foo?bar=20")
          (roundtrip-get wrapped {:async? true} "/foo?bar=20")))
    (is (= {:headers {}
            :status 200
            :body "{\"bar\" [\"20\" \"30\"]}"}
          (roundtrip-get wrapped {} "/foo?bar=20&bar=30")
          (roundtrip-get wrapped {:async? true} "/foo?bar=20&bar=30")))))


(deftest test-unexpected->500
  (let [respond-not-map (fn
                          ([request] "hey")
                          ([request respond raise] (respond "hey")))
        respond-bad-map (fn
                          ([request] {:foo 10})
                          ([request respond raise] {:foo 10}))
        respond-200-bad (fn
                          ([request] {:status 200})
                          ([request respond raise] (respond {:status 200})))
        respond-throwex (fn
                          ([request] (throw (Exception. "test")))
                          ([request respond raise] (throw (Exception. "test"))))]
    (is (= {:status 500
            :headers {"Content-Type" "text/plain"}
            :body "500 Internal Server Error"}
          (roundtrip-get (-> respond-not-map
                           (wrapper/unexpected->500-wrapper {:bract.core/config {}})) {} "/")
          (roundtrip-get (-> respond-not-map
                           (wrapper/unexpected->500-wrapper {:bract.core/config {}})) {:async true} "/")))
    (is (= {:status 500
            :headers {"Content-Type" "text/plain"}
            :body "500 Internal Server Error"}
          (roundtrip-get (-> respond-bad-map
                           (wrapper/unexpected->500-wrapper {:bract.core/config {}})) {} "/")
          (roundtrip-get (-> respond-bad-map
                           (wrapper/unexpected->500-wrapper {:bract.core/config {}})) {:async true} "/")))
    (is (= {:status 500
            :headers {"Content-Type" "text/plain"}
            :body "500 Internal Server Error"}
          (roundtrip-get (-> respond-200-bad
                           (wrapper/unexpected->500-wrapper {:bract.core/config {}})) {} "/")
          (roundtrip-get (-> respond-200-bad
                           (wrapper/unexpected->500-wrapper {:bract.core/config {}})) {:async true} "/")))
    (is (= {:status 500
            :headers {"Content-Type" "text/plain"}
            :body "500 Internal Server Error"}
          (roundtrip-get (-> respond-throwex
                           (wrapper/unexpected->500-wrapper {:bract.core/config {}})) {} "/")
          (roundtrip-get (-> respond-throwex
                           (wrapper/unexpected->500-wrapper {:bract.core/config {}})) {:async true} "/")))))


(deftest test-traffic-drain
  (let [sd-flag (volatile! false)
        context {:bract.core/config {}
                 :bract.core/shutdown-flag sd-flag}
        wrapped (-> handler
                  (wrapper/traffic-drain-wrapper context))]
    (is (= {:headers {"Content-Type" "text/plain"}
            :status 200
            :body "default"}
          (roundtrip-get wrapped {} "/")))
    (vreset! sd-flag true)
    (is (= {:headers {"Content-Type" "text/plain"}
            :status 503
            :body "503 Service Unavailable. Traffic draining is in progress."}
          (roundtrip-get wrapped {} "/")))))
