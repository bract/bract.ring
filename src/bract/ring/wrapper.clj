;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.wrapper
  (:require
    [clojure.string          :as string]
    [bract.core.keydef       :as core-kdef]
    [bract.core.type         :as core-type]
    [bract.core.util         :as core-util]
    [bract.core.util.runtime :as bcu-runtime]
    [bract.ring.keydef       :as ring-kdef]))


;; ----- /info -----


(defn info-response
  "Return /info response."
  [body-encoder content-type]
  {:status 200
   :headers {"Content-Type" content-type
             "Cache-Control" "no-store, no-cache, must-revalidate"}
   :body (body-encoder (bcu-runtime/sysinfo))})


(defn info-wrapper
  ([handler context]
    (info-wrapper handler context {}))
  ([handler context {:keys [uris
                            body-encoder
                            content-type]
                     :or {uris         #{"/info" "/info/"}
                          body-encoder pr-str
                          content-type "application/edn"}
                     :as options}]
    (let [uri-set      (set uris)
          body-encoder (core-type/ifunc body-encoder)
          info-process (fn [request]
                         (let [method (:request-method request)]
                           (if (= :get method)
                             (info-response body-encoder content-type)
                             {:status 405
                              :body (str "Expected HTTP GET request for info endpoint, but found "
                                      (-> method
                                        core-util/as-str
                                        string/upper-case))
                              :headers {"Content-Type" "text/plain"}})))]
      (fn
        ([request]
          (if (contains? uri-set (:uri request))
            (info-process request)
            (handler request)))
        ([request respond raise]
          (if (contains? uri-set (:uri request))
            (respond (info-process request))
            (handler request respond raise)))))))


;; ----- /ping -----


(defn ping-wrapper
  "Given Ring handler and Bract context, wrap the handler such that ping (default: /ping and /ping/) URIs lead to
  returning a ping response."
  ([handler context]
    (ping-wrapper handler context #{"/ping" "/ping/"}))
  ([handler context ping-uris]
    (ping-wrapper handler context ping-uris "pong"))
  ([handler context ping-uris body]
    (let [ping-uris-set (set ping-uris)
          ping-response (fn [] {:status 200
                                :body (str body)
                                :headers {"Content-Type"  "text/plain"
                                          "Cache-Control" "no-store, no-cache, must-revalidate"}})]
      (fn
        ([request]
          (if (->> (:uri request)
                (contains? ping-uris-set))
            (ping-response)
            (handler request)))
        ([request respond raise]
          (if (->> (:uri request)
                (contains? ping-uris-set))
            (respond (ping-response))
            (handler request respond raise)))))))


;; ----- request update (URI trailing slash) -----


(defn add-uri-trailing-slash
  "Given a request map, add any missing trailing slash to the URI and return updated request map."
  [request]
  (let [^String uri (:uri request)
        uri-lastidx (unchecked-dec (.length uri))]
    (if (identical? \/ (.charAt uri uri-lastidx))
      request
      (assoc request :uri (let [^StringBuilder sb (StringBuilder. uri)]
                            (.append sb \/)
                            (.toString sb))))))


(defn remove-uri-trailing-slash
  "Given a request map, remove any trailing slash from the URI and return updated request map."
  [request]
  (let [^String uri (:uri request)
        uri-lastidx (unchecked-dec (.length uri))]
    (if (identical? \/ (.charAt uri uri-lastidx))
      (assoc request :uri (subs uri 0 uri-lastidx))
      request)))


(defn request-update-wrapper
  "Given Ring handler, Bract context and request-updater function `(fn [request]) -> request`, wrap the handler such
  that the request is updated before the Ring handler is applied to it."
  [handler context request-updater]
  (let [request-updater-fn (core-type/ifunc request-updater)]
    (fn
      ([request]
        (-> request
          request-updater-fn
          handler))
      ([request respond raise]
        (-> request
          request-updater-fn
          (handler respond raise))))))


;; ----- URI prefix -----


(defn make-uri-prefix-matcher
  "Given a URI prefix, a flag to decide whether to strip the prefix and a key (nil=disabled) to backup the old URI to,
  return a function `(fn [request]) -> request?` returning updated request on successful prefix match, nil otherwise.
  See: uri-prefix-match-wrapper"
  [^String uri-prefix strip-uri? backup-key]
  (let [n (.length uri-prefix)
        backup-uri (if (some? backup-key)
                     (fn [request] (assoc request backup-key (:uri request)))
                     identity)
        sub-uri    (fn ^String [^String uri] (subs uri n))
        strip-uri  (if strip-uri?
                     (fn [request] (update request :uri sub-uri))
                     identity)]
    (fn [request]
      (let [^String uri (:uri request)]
        (when (and (.startsWith uri uri-prefix)
                (> (.length uri) n))
          ;; return potentially updated request on success, nil otherwise
          (-> request
            backup-uri
            strip-uri))))))


(defn uri-prefix-match-wrapper
  "Given a Ring handler, a Bract context, a URI prefix, a flag to decide whether to strip the prefix and a key (nil
  implies no-backup) to backup the old URI to, return updated Ring handler that matches prefix and proceeds on success
  or returns HTTP 400 on no match.
  See: make-uri-prefix-matcher"
  [handler context ^String uri-prefix strip-uri? backup-key]
  (let [matcher (make-uri-prefix-matcher ^String uri-prefix strip-uri? backup-key)
        res-400 {:status 400
                 :body (format "Expected URI to start with and be longer than '%s'" uri-prefix)
                 :headers {"Content-Type" "text/plain"}}]
    (fn
      ([request]
        (if-let [request (matcher request)]
          (handler request)
          res-400))
      ([request respond raise]
        (if-let [request (matcher request)]
          (handler request respond raise)
          (respond res-400))))))


(defn wrap-params-normalize-wrapper
  "Normalize the result of `wrap-params` middleware (this middleware may be invoked only after `wrap-params`) by
  transforming each request params value. The `wrap-params` middleware extracts params as string, but multiple values
  for same param are turned into a vector of string - this middleware turns all param values into vectors of string
  and applies normalizer to that vector.

  Without this middleware:
  {\"foo\" \"bar\"
   \"baz\" [\"quux\" \"corge\"]}

  After using this middleware, where normalizer is `clojure.core/first`:
  {\"foo\" \"bar\"
   \"baz\" \"quux\"}"
  [handler context normalizer]
  (let [normalize (comp (core-type/ifunc normalizer) (fn [x] (cond
                                                               (vector? x) x
                                                               (coll? x)   (vec x)
                                                               :otherwise  [x])))
        transform (fn [m k]
                    (if-let [pairs (get m k)]
                      (->> pairs
                        (reduce-kv (fn [mm kk vv] (assoc mm kk (normalize vv))) {})
                        (assoc m k))
                      m))]
    (fn
      ([request]
        (handler (-> request
                   (transform :form-params)
                   (transform :query-params)
                   (transform :params))))
      ([request respond raise]
        (handler (-> request
                   (transform :form-params)
                   (transform :query-params)
                   (transform :params))
          respond raise)))))


(defn bad-response->500 [request response reason]
  {:status 500
   :headers {"Content-Type" "text/plain"}
   :body "500 Internal Server Error"})


(defn bad-response->verbose-500 [request response reason]
  {:status 500
   :headers {"Content-Type" "text/plain"}
   :body (format "500 Internal Server Error

Class: %s
Response: %s
Request: %s
Reason: %s"
           (class response)
           (pr-str response)
           (pr-str request)
           reason)})


(defn exception->500 [request ^Throwable thrown]
  {:status 500
   :headers {"Content-Type" "text/plain"}
   :body "500 Internal Server Error"})


(defn exception->verbose-500 [request ^Throwable thrown]
  {:status 500
   :headers {"Content-Type" "text/plain"}
   :body (format "500 Internal Server Error

%s
Request: %s"
           (core-util/stack-trace-str thrown)
           (pr-str request))})


(defn unexpected->500-wrapper
  "Wrap given Ring handler such that if it returns unexpected Ring response (invalid/malformed response or exception)
  then return HTTP 500 Ring response."
  ([handler context]
    (unexpected->500-wrapper handler context false))
  ([handler context {:keys [on-bad-response
                            on-exception]
                     :or {on-bad-response bad-response->500
                          on-exception    exception->500}
                     :as options}]
    (let [on-bad-response (core-type/ifunc on-bad-response)
          on-exception    (core-type/ifunc on-exception)
          unexpected->500 (fn [request response]
                            (let [status (:status response)
                                  body   (:body response)]
                              (if (and (map? response)
                                    (integer? status)
                                    (<= 100 status 599))
                                (cond
                                  body       response
                                  (= 200
                                    status)  (on-bad-response
                                               request response
                                               "Response map has HTTP status 200 but body is missing")
                                  :otherwise response)
                                (on-bad-response
                                  request response
                                  "Expected Ring response to be a map with :status key and integer value"))))]
      (fn
        ([request]
          (try
            (unexpected->500 request (handler request))
            (catch Throwable e
              (on-exception request e))))
        ([request respond raise]
          (let [new-respond (fn [response]
                              (respond (unexpected->500 request response)))]
            (try
              (handler request new-respond raise)
              (catch Throwable e
                (respond (on-exception request e))))))))))


(defn traffic-drain-wrapper
  "Given a deref'able shutdown state and a boolean flag to respond with connection-close HTTP header, wrap specified
  Ring handler to respond with HTTP 503 (in order to drain current traffic) when the system is shutting down."
  ([handler context]
    (traffic-drain-wrapper handler context {}))
  ([handler context {:keys [conn-close?]
                     :or {conn-close? true}}]
    (let [shutdown-flag (core-kdef/ctx-shutdown-flag context)
          response-503  (let [response {:status 503
                                        :headers {"Content-Type" "text/plain"}
                                        :body "503 Service Unavailable. Traffic draining is in progress."}]
                          (if conn-close?
                            (assoc-in response [:headers "Connection"] "close")
                            response))
          alive-tracker (core-kdef/ctx-alive-tstamp context)]
      ;; initialize alive-tracker
      (alive-tracker)
      ;; return wrapped handler
      (fn [request]
        (if @shutdown-flag
          response-503
          (try
            (let [response (handler request)]
              (if (and conn-close?
                    @shutdown-flag
                    (map? response)
                    (integer? (:status response)))
                (assoc-in response [:headers "Connection"] "close")
                response))
            (finally
              ;; record last service time
              (alive-tracker))))))))


(defn health-check-response
  "Return health-check response."
  [health-check-fns health-http-codes body-encoder content-type]
  (let [health-result (->> health-check-fns
                        (mapv #(%))
                        core-util/health-status)
        http-status   (or (->> (:status health-result)
                            (get health-http-codes))
                        200)]
    {:status http-status
     :headers {"Content-Type" content-type}
     :body (body-encoder health-result)}))


(defn health-check-wrapper
  "Given optional URIs (default: /health), body encoder (default: EDN) and content type (default: application/edn),
  wrap specified Ring handler such that it responds to application health query when health endpoint is requested."
  ([handler context]
    (health-check-wrapper handler context {}))
  ([handler context {:keys [uris
                            body-encoder
                            content-type]
                     :or {uris #{"/health" "/health/"}
                          body-encoder pr-str
                          content-type "application/edn"}
                     :as options}]
    (let [body-encoder (core-type/ifunc body-encoder)
          uri-set  (set uris)
          hc-fns   (core-kdef/ctx-health-check context)
          hc-codes (->> context
                     core-kdef/ctx-config
                     ring-kdef/cfg-health-codes
                     (merge {:critical 503
                             :degraded 500
                             :healthy  200}))
          checknow (fn [request]
                     (let [method (:request-method request)]
                       (if (= :get method)
                         (health-check-response hc-fns hc-codes body-encoder content-type)
                         {:status 405
                          :body (str "Expected HTTP GET request for health check endpoint, but found "
                                  (-> method
                                    core-util/as-str
                                    string/upper-case))
                          :headers {"Content-Type" "text/plain"}})))]
      (fn
        ([request]
          (if (contains? uri-set (:uri request))
            (checknow request)
            (handler request)))
        ([request respond raise]
          (if (contains? uri-set (:uri request))
            (respond (checknow request))
            (handler request respond raise)))))))
