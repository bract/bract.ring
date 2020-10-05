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
    [java.io ByteArrayInputStream]
    [org.eclipse.jetty.server Server]))


(def default-config (core-kdef/resolve-config {} ["bract/ring/config.edn"]))


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
  ([handler jetty-options path http-options]
    (with-jetty handler (conj {:port 3000 :join? false}
                          jetty-options)
      (-> (str "http://localhost:3000" path)
        (client/get (merge {:throw-exceptions false} http-options))
        (dissoc
          :trace-redirects :length :orig-content-encoding
          :request-time :repeatable? :protocol-version
          :streaming? :chunked? :reason-phrase)
        (update-in [:headers] dissoc "Date" "Connection" "Server"))))
  ([handler jetty-options path]
    (roundtrip-get handler jetty-options path {})))


(defn roundtrip-post
  ([handler jetty-options path]
    (roundtrip-post handler jetty-options path nil))
  ([handler jetty-options path http-options]
    (with-jetty handler (conj {:port 3000 :join? false}
                          jetty-options)
      (-> (str "http://localhost:3000" path)
        (client/post (merge {:throw-exceptions false} http-options))
        (dissoc
          :trace-redirects :length :orig-content-encoding
          :request-time :repeatable? :protocol-version
          :streaming? :chunked? :reason-phrase)
        (update-in [:headers] dissoc "Date" "Connection" "Server")))))


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
                      (wrapper/health-check-wrapper {:bract.core/config default-config}
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
                           (wrapper/health-check-wrapper {:bract.core/config default-config
                                                          :bract.core/health-check [(fn [] mysql-broken)]}
                             {:body-encoder body-encoder
                              :content-type content-type}))
            degraded-hnd (-> handler
                           (wrapper/health-check-wrapper {:bract.core/config default-config
                                                          :bract.core/health-check [(fn [] mysql-status)
                                                                                    (fn [] cache-status)
                                                                                    (fn [] disk-status)]}
                             {:body-encoder body-encoder
                              :content-type content-type}))
            healthy-hnd  (-> handler
                           (wrapper/health-check-wrapper {:bract.core/config default-config
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
    (let [wrapped-handler (wrapper/info-endpoint-wrapper handler {:bract.core/config default-config}
                            {:body-encoder body-encoder
                             :content-type content-type})
          wrapped-custom  (wrapper/info-endpoint-wrapper handler {:bract.core/config default-config
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
  (let [wrapped-handler (wrapper/ping-endpoint-wrapper handler {:bract.core/config default-config})]
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
              :body)))
      (is (= "song"
            (-> (wrapped-handler {:uri "/ping" :request-method :post :body (-> "song"
                                                                             (.getBytes)
                                                                             (ByteArrayInputStream.))})
              :body)
            (-> (roundtrip-post wrapped-handler {} "/ping" {:body "song"
                                                            ;; :headers {"Content-type" "text/plain"}
                                                            })
              :body)
            (-> (roundtrip-post wrapped-handler {:async? true} "/ping" {:body "song"
                                                                        :headers {"Content-type" "text/plain"}})
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
              :body)))
      (is (= "song"
            (-> (wrapped-handler {:uri "/ping" :request-method :post :body (-> "song"
                                                                             (.getBytes)
                                                                             (ByteArrayInputStream.))})
              :body)
            (-> (roundtrip-post wrapped-handler {} "/ping" {:body "song"
                                                            ;; :headers {"Content-type" "text/plain"}
                                                            })
              :body)
            (-> (roundtrip-post wrapped-handler {:async? true} "/ping" {:body "song"
                                                                        :headers {"Content-type" "text/plain"}})
              :body))))))


(deftest test-uri-trailing-space
  (let [handler (fn
                  ([request] {:status 200
                              :body (:uri request)})
                  ([request respond raise] (respond {:status 200
                                                     :body (:uri request)})))
        add-trs (wrapper/uri-trailing-slash-wrapper handler
                  {:bract.core/config (assoc default-config
                                        "bract.ring.uri.trailing.slash.enabled" true
                                        "bract.ring.uri.trailing.slash.action"  :add)})
        rem-trs (wrapper/uri-trailing-slash-wrapper handler
                  {:bract.core/config (assoc default-config
                                        "bract.ring.uri.trailing.slash.enabled" true
                                        "bract.ring.uri.trailing.slash.action"  :remove)})]
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
             response] [[(wrapper/make-uri-prefix-matcher "/prefix" true  true  :backup)  {:uri "/foo"
                                                                                           :backup "/prefix/foo"}]
                        [(wrapper/make-uri-prefix-matcher "/prefix" false true  :backup) {:uri "/prefix/foo"
                                                                                          :backup "/prefix/foo"}]
                        [(wrapper/make-uri-prefix-matcher "/prefix" false false nil)     {:uri "/prefix/foo"}]]]
      (is (= response
            (matcher {:uri "/prefix/foo"})))
      (is (= nil
            (matcher {:uri "/foo/bar"})))
      (is (= nil
            (matcher {:uri "/prefix"})))))
  (testing "wrapper"
    (let [wrapped (wrapper/uri-prefix-match-wrapper handler
                    {:bract.core/config (assoc default-config
                                          "bract.ring.uri.prefix.match.enabled" true
                                          "bract.ring.uri.prefix.match.token"   "/prefix"
                                          "bract.ring.uri.prefix.backup.key"    :backup)})]
      (is (= {:status 200
              :body "default"
              :headers {"Content-Type" "text/plain"}}
            (wrapped {:uri "/prefix/foo"})))
      (is (= {:status 400
              :body "Expected URI to start with and be longer than '/prefix'"
              :headers {"Content-Type" "text/plain"}}
            (wrapped {:uri "/foo"}))))))


(deftest test-params-normalize
  (let [handler (fn params-as-body
                  ([request] {:status 200
                              :body (str (:params request))})
                  ([request respond raise] (respond (params-as-body request))))
        wrapped (-> handler
                  (wrapper/params-normalize-wrapper {:bract.core/config default-config})
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
                           (wrapper/unexpected->500-wrapper {:bract.core/config default-config})) {} "/")
          (roundtrip-get (-> respond-not-map
                           (wrapper/unexpected->500-wrapper {:bract.core/config default-config})) {:async true} "/")))
    (is (= {:status 500
            :headers {"Content-Type" "text/plain"}
            :body "500 Internal Server Error"}
          (roundtrip-get (-> respond-bad-map
                           (wrapper/unexpected->500-wrapper {:bract.core/config default-config})) {} "/")
          (roundtrip-get (-> respond-bad-map
                           (wrapper/unexpected->500-wrapper {:bract.core/config default-config})) {:async true} "/")))
    (is (= {:status 500
            :headers {"Content-Type" "text/plain"}
            :body "500 Internal Server Error"}
          (roundtrip-get (-> respond-200-bad
                           (wrapper/unexpected->500-wrapper {:bract.core/config default-config})) {} "/")
          (roundtrip-get (-> respond-200-bad
                           (wrapper/unexpected->500-wrapper {:bract.core/config default-config})) {:async true} "/")))
    (is (= {:status 500
            :headers {"Content-Type" "text/plain"}
            :body "500 Internal Server Error"}
          (roundtrip-get (-> respond-throwex
                           (wrapper/unexpected->500-wrapper {:bract.core/config default-config})) {} "/")
          (roundtrip-get (-> respond-throwex
                           (wrapper/unexpected->500-wrapper {:bract.core/config default-config})) {:async true} "/")))))


(deftest test-traffic-drain
  (let [sd-flag (volatile! false)
        wrapped (-> handler
                  (wrapper/traffic-drain-wrapper {:bract.core/config default-config
                                                  :bract.core/*shutdown-flag sd-flag}))]
    (is (= {:headers {"Content-Type" "text/plain"}
            :status 200
            :body "default"}
          (roundtrip-get wrapped {} "/")
          (roundtrip-get wrapped {:async true} "/")))
    (vreset! sd-flag true)
    (is (= {:headers {"Content-Type" "text/plain"
                      "Connection" "close"}
            :status 503
            :body "503 Service Unavailable. Traffic draining is in progress."}
          ;; avoid HTTP roundtrip here because the web server doesn't propagate the 'Connection: close' header
          (wrapped {:uri "/"})
          (wrapped {:uri "/"} identity #(throw %)))))
  (let [sd-flag (volatile! false)
        wrapped (-> handler
                  (wrapper/traffic-drain-wrapper {:bract.core/config (assoc default-config
                                                                       "bract.ring.traffic.conn.close.flag" false)
                                                  :bract.core/*shutdown-flag sd-flag}))]
    (is (= {:headers {"Content-Type" "text/plain"}
            :status 200
            :body "default"}
          (roundtrip-get wrapped {} "/")
          (roundtrip-get wrapped {:async true} "/")))
    (vreset! sd-flag true)
    (is (= {:headers {"Content-Type" "text/plain"}
            :status 503
            :body "503 Service Unavailable. Traffic draining is in progress."}
          (roundtrip-get wrapped {} "/")
          (roundtrip-get wrapped {:async true} "/")))))


(defn valid-id
  [id]
  (when (< (count id) 4) "ID too short"))


(deftest test-distributed-trace
  (let [handler (fn self
                  ([request]
                    {:status 200
                     :body (cheshire/generate-string {:trace-id  (:trace-id request)
                                                      :span-id   (:span-id request)
                                                      :parent-id (:parent-id request)})})
                  ([request respond raise]
                    (respond (self request))))
        wrapped (-> handler
                  (wrapper/distributed-trace-wrapper {:bract.core/config default-config}))
        needhdr (-> handler
                  (wrapper/distributed-trace-wrapper {:bract.core/config (assoc default-config
                                                                           "bract.ring.trace.trace.id.req.flag" true)}))
        nlength (-> handler
                  (wrapper/distributed-trace-wrapper {:bract.core/config (assoc default-config
                                                                           "bract.ring.trace.trace.id.valid.fn"
                                                                           #'valid-id)}))]
    (doseq [response [(roundtrip-get wrapped {} "/")
                      (roundtrip-get wrapped {:async true} "/")]]
      (let [data (-> response
                   :body
                   cheshire/parse-string)]
        (is (= 32 (count (get data "trace-id"))))
        (is (= 32 (count (get data "span-id"))))
        (is (nil? (get data "parent-id")))))
    (doseq [response [(roundtrip-get wrapped {} "/" {:headers {"x-trace-id" "1234"}})
                      (roundtrip-get needhdr {} "/" {:headers {"x-trace-id" "1234"}})
                      (roundtrip-get nlength {} "/" {:headers {"x-trace-id" "1234"}})
                      (roundtrip-get wrapped {:async true} "/" {:headers {"x-trace-id" "1234"}})
                      (roundtrip-get needhdr {:async true} "/" {:headers {"x-trace-id" "1234"}})
                      (roundtrip-get nlength {:async true} "/" {:headers {"x-trace-id" "1234"}})]]
      (let [data (-> response
                   :body
                   cheshire/parse-string)]
        (is (= {"trace-id" "1234"
                "parent-id" nil}
              (select-keys data ["trace-id" "parent-id"])))))
    (is (= {:status 400
            :headers {"Content-Type" "text/plain"}
            :body "400 Missing Trace-ID

Every request must bear the header 'x-trace-id'"}
          (roundtrip-get needhdr {} "/")
          (roundtrip-get needhdr {:async true} "/")))
    (is (= {:status 400
            :headers {"Content-Type" "text/plain"}
            :body "400 Invalid Trace-ID

Header 'x-trace-id' has invalid value: ID too short"}
          (roundtrip-get nlength {} "/" {:headers {"x-trace-id" "123"}})
          (roundtrip-get nlength {:async true} "/" {:headers {"x-trace-id" "123"}})))))


(deftest test-traffic-log
  (let [request-logger   (atom nil)
        response-logger  (atom nil)
        exception-logger (atom nil)
        reset-loggers!   (fn []
                           (reset! request-logger nil)
                           (reset! response-logger nil)
                           (reset! exception-logger nil))
        happy-handler (fn self
                        ([request]               {:status 200 :body "OK"})
                        ([request respond raise] (respond (self request))))
        error-handler (fn self
                        ([request]               (throw (Exception. "test")))
                        ([request respond raise] (raise (Exception. "test"))))
        wrap-handler  (fn [f]
                        (-> f
                          (wrapper/traffic-log-wrapper {:bract.core/config default-config}
                            {:request-logger (fn [request] (reset! request-logger :called))
                             :response-logger (fn [request response ^double duration-millis]
                                                (reset! response-logger :called))
                             :exception-logger (fn [request response ^double duration-millis]
                                                 (reset! exception-logger :called))})))]
    (testing "happy sync"
      (reset-loggers!)
      (roundtrip-get (wrap-handler happy-handler) {} "/" {})
      (is (= :called
            @request-logger
            @response-logger))
      (is (nil?
            @exception-logger)))
    (testing "error sync"
      (reset-loggers!)
      (roundtrip-get (wrap-handler error-handler) {} "/" {})
      (is (= :called
            @request-logger
            @exception-logger))
      (is (nil?
            @response-logger)))
    (testing "happy async"
      (reset-loggers!)
      (roundtrip-get (wrap-handler happy-handler) {:async true} "/" {})
      (is (= :called
            @request-logger
            @response-logger))
      (is (nil?
            @exception-logger)))
    (testing "error sync"
      (reset-loggers!)
      (roundtrip-get (wrap-handler error-handler) {:async true} "/" {})
      (is (= :called
            @request-logger
            @exception-logger))
      (is (nil?
            @response-logger)))))
