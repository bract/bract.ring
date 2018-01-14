;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.wrapper
  (:require
    [clojure.pprint :as pp]
    [bract.core.type         :as core-type]
    [bract.core.util         :as core-util]
    [bract.core.util.runtime :as bcu-runtime]))


;; ----- /info -----


(defn info-edn-handler
  "Return system info as Ring response containing EDN body string."
  ([]
    {:status 200
     :body (-> (bcu-runtime/sysinfo)
             pp/pprint
             with-out-str)
     :headers {"Content-Type"  "application/edn"
               "Cache-Control" "no-store, no-cache, must-revalidate"}})
  ([request] (info-edn-handler))
  ([request respond raise] (respond (info-edn-handler))))


(defn info-edn-wrapper
  "Given Ring handler and Bract context, wrap the handler such that info (default: /info/edn and /info/edn/) URIs
  lead to returning system info as EDN string body of the response."
  ([handler context]
    (info-edn-wrapper handler context #{"/info/edn" "/info/edn/"}))
  ([handler context info-uris]
    (let [info-uris-set (set info-uris)]
      (fn info-edn
        ([request]
          (if (->> (:uri request)
                (contains? info-uris-set))
            (info-edn-handler)
            (handler request)))
        ([request respond raise]
          (if (->> (:uri request)
                (contains? info-uris-set))
            (respond (info-edn-handler))
            (handler request respond raise)))))))


(defn make-info-json-handler
  "Given JSON encoder function, make a Ring handler function that returns system info as Ring response containing
  JSON body string."
  [json-encoder]
  (fn info-json
    ([]
      {:status 200
       :body (json-encoder (bcu-runtime/sysinfo))
       :headers {"Content-Type"  "application/json"
                 "Cache-Control" "no-store, no-cache, must-revalidate"}})
    ([request] (info-json))
    ([request respond raise] (respond (info-json)))))


(defn info-json-wrapper
  "Given Ring handler, Bract context and JSON encoder function, wrap the handler such that info (default: /info/json
  and /info/json/) URIs lead to returning system info as JSON string body of the response."
  ([handler context json-encoder]
    (info-json-wrapper handler context json-encoder #{"/info/json" "/info/json/"}))
  ([handler context json-encoder info-uris]
    (let [info-uris-set (set info-uris)
          gen-response  (make-info-json-handler json-encoder)]
      (fn
        ([request]
          (if (->> (:uri request)
                (contains? info-uris-set))
            (gen-response)
            (handler request)))
        ([request respond raise]
          (if (->> (:uri request)
                (contains? info-uris-set))
            (respond (gen-response))
            (handler request respond raise)))))))


;; ----- ping -----


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
