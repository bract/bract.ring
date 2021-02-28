;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.wrapper
  "A bract.ring _wrapper_ is a function `(fn [ring-handler context & args]) -> ring-handler` to wrap the handler (like
  a middleware) using information in the context. This namespace includes such wrappers and some helper functions."
  (:require
    [clojure.java.io         :as io]
    [clojure.string          :as string]
    [bract.core.keydef       :as core-kdef]
    [bract.core.type         :as core-type]
    [bract.core.util         :as core-util]
    [bract.core.util.runtime :as bcu-runtime]
    [bract.ring.keydef       :as ring-kdef]
    [bract.ring.middleware   :as ring-mware])
  (:import
    [java.io InputStream]))


(defmacro when-wrapper-enabled
  "Given a predicate function, handler and context, evaluate body of code only when `(pred config)` returns truthy,
  return the handler otherwise. The config is extracted from context before applying the predicate to it."
  [pred handler context & body]
  `(let [config# (core-kdef/ctx-config ~context)]
     (if (~pred config#)
       (do ~@body)
       ~handler)))


(defmacro opt-or-config
  "Assuming 'options and 'context symbols are bound to respective arguments, extract the value of specified option
  falling back to config."
  [opt-key config-f]
  `(if (contains? ~'options ~opt-key)
     (get ~'options ~opt-key)
     (->> ~'context
       core-kdef/ctx-config
       ~config-f)))


;; ----- /health check -----


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
  "Given optional URIs (default: `/health`), body encoder (default: EDN) and content type (default: `application/edn`),
  wrap specified Ring handler such that it responds to application health query when health endpoint is requested.

  | Option        | Config key                         | Default config value                       |
  |---------------|------------------------------------|--------------------------------------------|
  |`:uris`        |`\"bract.ring.health.check.uris\"`  |`[\"/health\" \"/health/\"]`                |
  |`:body-encoder`|`\"bract.ring.health.body.encoder\"`|[[clojure.core/pr-str]]                     |
  |`:content-type`|`\"bract.ring.health.content.type\"`|`\"application/edn\"`                       |
  |`:http-codes`  |`\"bract.ring.health.http.codes\"`  |`{:critical 503 :degraded 500 :healthy 200}`|"
  ([handler context]
    (health-check-wrapper handler context {}))
  ([handler context options]
    (when-wrapper-enabled ring-kdef/cfg-health-check-wrapper? handler context
      (let [uri-set      (->> ring-kdef/cfg-health-check-uris
                           (opt-or-config :uris)
                           set)
            body-encoder (->> ring-kdef/cfg-health-body-encoder
                           (opt-or-config :body-encoder)
                           core-type/ifunc)
            content-type (->> ring-kdef/cfg-health-content-type
                           (opt-or-config :content-type))
            hc-functions (core-kdef/ctx-health-check context)
            http-codes   (->> ring-kdef/cfg-health-http-codes
                           (opt-or-config :http-codes)
                           (merge {:critical 503
                                   :degraded 500
                                   :healthy  200}))
            event-name    (-> context
                            core-kdef/ctx-config
                            ring-kdef/cfg-health-event-name)
            event-logger  (core-kdef/resolve-event-logger context event-name)
            check-now    (fn [request]
                           (let [method (:request-method request)]
                             (if (= :get method)
                               (core-util/doafter
                                 (health-check-response hc-functions http-codes body-encoder content-type)
                                 (event-logger event-name))
                               {:status 405
                                :body (str "Expected HTTP GET request for health check endpoint, but found "
                                        (-> method
                                          core-util/as-str
                                          string/upper-case))
                                :headers {"Content-Type" "text/plain"}})))]
        (fn
          ([request]
            (if (contains? uri-set (:uri request))
              (check-now request)
              (handler request)))
          ([request respond raise]
            (if (contains? uri-set (:uri request))
              (respond (check-now request))
              (handler request respond raise))))))))


;; ----- /info -----


(defn info-response
  "Return /info response."
  [info-gen-fns body-encoder content-type]
  {:status 200
   :headers {"Content-Type" content-type
             "Cache-Control" "no-store, no-cache, must-revalidate"}
   :body (body-encoder (bcu-runtime/runtime-info info-gen-fns))})


(defn info-endpoint-wrapper
  "Given Ring handler and Bract context, wrap the handler such that info (default: `/info` and `/info/`) URIs lead to
  returning a runtime info response.

  | Option        | Config key                            | Default config value   |
  |---------------|---------------------------------------|------------------------|
  |`:uris`        |`\"bract.ring.info.endpoint.uris\"`    |`[\"/info\" \"/info/\"]`|
  |`:body-encoder`|`\"bract.ring.info.body.encoder\"`     |[[clojure.core/pr-str]] |
  |`:content-type`|`\"bract.ring.info.body.content.type\"`|`\"application/edn\"`   |"
  ([handler context]
    (info-endpoint-wrapper handler context {}))
  ([handler context options]
    (when-wrapper-enabled ring-kdef/cfg-info-endpoint-wrapper? handler context
      (let [info-uri-set (->> ring-kdef/cfg-info-endpoint-uris
                           (opt-or-config :uris)
                           set)
            body-encoder (->> ring-kdef/cfg-info-body-encoder
                           (opt-or-config :body-encoder)
                           core-type/ifunc)
            content-type (->> ring-kdef/cfg-info-content-type
                           (opt-or-config :content-type))
            info-gen-fns (core-kdef/ctx-runtime-info context)
            event-name    (-> context
                            core-kdef/ctx-config
                            ring-kdef/cfg-info-event-name)
            event-logger  (core-kdef/resolve-event-logger context event-name)
            info-process (fn [request]
                           (let [method (:request-method request)]
                             (if (= :get method)
                               (core-util/doafter
                                 (info-response info-gen-fns body-encoder content-type)
                                 (event-logger event-name))
                               {:status 405
                                :body (str "Expected HTTP GET request for info endpoint, but found "
                                        (-> method
                                          core-util/as-str
                                          string/upper-case))
                                :headers {"Content-Type" "text/plain"}})))]
        (fn
          ([request]
            (if (contains? info-uri-set (:uri request))
              (info-process request)
              (handler request)))
          ([request respond raise]
            (if (contains? info-uri-set (:uri request))
              (respond (info-process request))
              (handler request respond raise))))))))


;; ----- /ping -----


(defn ping-endpoint-wrapper
  "Given Ring handler and Bract context, wrap the handler such that ping (default: `/ping` and `/ping/`) URIs lead to
  returning a ping response.

  | Option        | Config key                        | Default config value   |
  |---------------|-----------------------------------|------------------------|
  |`:uris`        |`\"bract.ring.ping.endpoint.uris\"`|`[\"/ping\" \"/ping/\"]`|
  |`:body`        |`\"bract.ring.ping.endpoint.body\"`|`\"pong\"`              |
  |`:content-type`|`\"bract.ring.ping.content.type\"` |`\"text/plain\"`        |"
  ([handler context]
    (ping-endpoint-wrapper handler context {}))
  ([handler context options]
    (when-wrapper-enabled ring-kdef/cfg-ping-endpoint-wrapper? handler context
      (let [ping-uri-set (->> ring-kdef/cfg-ping-endpoint-uris
                           (opt-or-config :uris)
                           set)
            body         (->> ring-kdef/cfg-ping-endpoint-body
                           (opt-or-config :body))
            content-type (->> ring-kdef/cfg-ping-content-type
                           (opt-or-config :content-type))
            ping-response {:status 200
                           :body body
                           :headers {"Content-Type"  content-type
                                     "Cache-Control" "no-store, no-cache, must-revalidate"}}
            event-name    (-> context
                            core-kdef/ctx-config
                            ring-kdef/cfg-ping-event-name)
            event-logger  (core-kdef/resolve-event-logger context event-name)
            find-response (fn [request]
                            (core-util/doafter
                              (let [method (:request-method request)]
                                (if (identical? :get method)  ; GET method
                                  ping-response
                                  (let [content-type (get (:headers request) "content-type")
                                        ct-token-set (if (some? content-type)
                                                       (->> ";|,"
                                                         (.split ^String content-type)
                                                         (map #(.trim ^String %))
                                                         set)
                                                       #{"text/plain"}) ; unspecified - text/plain
                                        request-body (:body request)]
                                    (if (and (identical? :post method)  ; POST method, and
                                          (ct-token-set "text/plain")   ; Content-type: text/plain
                                          (instance? InputStream request-body))
                                      (assoc ping-response
                                        :body (slurp request-body))
                                      ping-response))))
                              (event-logger event-name)))]
        (fn
          ([request]
            (if (->> (:uri request)
                  (contains? ping-uri-set))
              (try (find-response request) (catch Exception e (.printStackTrace e)))
              (handler request)))
          ([request respond raise]
            (if (->> (:uri request)
                  (contains? ping-uri-set))
              (respond (find-response request))
              (handler request respond raise))))))))


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
        uri-length  (.length uri)
        uri-lastidx (unchecked-dec uri-length)]
    (if (> uri-length 1)
      (if (identical? \/ (.charAt uri uri-lastidx))
        (assoc request :uri (subs uri 0 uri-lastidx))
        request)
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


(defn uri-trailing-slash-wrapper
  "Wrap handler such that the trailing slash is either added (if missing) or removed (if present) depending upon the
  specified optional action (:add/:remove).

  | Option        | Config key                               | Default config value |
  |---------------|------------------------------------------|----------------------|
  |`:action`      |`\"bract.ring.uri.trailing.slash.action\"`|`:remove`             |"
  ([handler context]
    (uri-trailing-slash-wrapper handler context {}))
  ([handler context options]
    (when-wrapper-enabled ring-kdef/cfg-uri-trailing-slash-wrapper? handler context
      (let [action (->> ring-kdef/cfg-uri-trailing-slash-action
                     (opt-or-config :action))]
        (case action
          :add    (request-update-wrapper handler context add-uri-trailing-slash)
          :remove (request-update-wrapper handler context remove-uri-trailing-slash)
          (core-util/expected "action keyword :add or :remove" action))))))


;; ----- URI prefix -----


(defn make-uri-prefix-matcher
  "Given a URI prefix, a flag to decide whether to strip the prefix and a key (`nil`=disabled) to backup the old URI to,
  return a function `(fn [request]) -> request?` returning updated request on successful prefix match, `nil` otherwise.

  See: [[uri-prefix-match-wrapper]]"
  [^String uri-prefix strip-prefix? uri-backup? backup-key]
  (let [n (.length uri-prefix)
        backup-uri (if uri-backup?
                     (fn [request] (assoc request backup-key (:uri request)))
                     identity)
        sub-uri    (fn ^String [^String uri] (subs uri n))
        strip-uri  (if strip-prefix?
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
  "Given a Ring handler, a Bract context, a URI prefix, a flag to decide whether to strip the prefix and a key (`nil`
  implies no-backup) to backup the old URI to, return updated Ring handler that matches prefix and proceeds on success
  or returns HTTP 400 on no match.

  | Option         | Config key                            | Default config value |
  |----------------|---------------------------------------|----------------------|
  |`:uri-prefix`   |`\"bract.ring.uri.prefix.match.token\"`| (required config)    |
  |`:strip-prefix?`|`\"bract.ring.uri.prefix.strip.flag\"` | `true`               |
  |`:backup-uri?`  |`\"bract.ring.uri.prefix.backup.flag\"`| `true`               |
  |`:backup-key`   |`\"bract.ring.uri.prefix.backup.key\"` | `:original-uri`      |

  See: [[make-uri-prefix-matcher]]"
  ([handler context]
    (uri-prefix-match-wrapper handler context {}))
  ([handler context options]
    (when-wrapper-enabled ring-kdef/cfg-uri-prefix-match-wrapper? handler context
      (let [uri-prefix    (->> ring-kdef/cfg-uri-prefix-match-token
                            (opt-or-config :uri-prefix))
            strip-prefix? (->> ring-kdef/cfg-uri-prefix-strip-prefix?
                            (opt-or-config :strip-prefix?))
            backup-uri?   (->> ring-kdef/cfg-uri-prefix-backup-uri?
                            (opt-or-config :backup-uri?))
            backup-key    (->> ring-kdef/cfg-uri-prefix-backup-key
                            (opt-or-config :backup-key))
            matcher (make-uri-prefix-matcher uri-prefix strip-prefix? backup-uri? backup-key)
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
              (respond res-400))))))))


;; ----- params normalize -----


(defn params-normalize-wrapper
  "Normalize the result of `ring.middleware.params/wrap-params` middleware (that is, the request map with `:params`) by
  transforming each request params value. The `wrap-params` middleware extracts params as string, but multiple values
  for same param are turned into a vector of string - this middleware turns all param values into vectors of string
  and applies normalizer to that vector.

  | Option      | Config key                               | Default config value     |
  |-------------|------------------------------------------|--------------------------|
  |`:normalizer`|`\"bract.ring.params.normalize.function\"`|[[clojure.core/identity]] |

  Without this middleware/wrapper:

  ```edn
  {\"foo\" \"bar\"
   \"baz\" [\"quux\" \"corge\"]}
  ```

  After using this middleware, where normalizer is `clojure.core/first`:

  ```edn
  {\"foo\" \"bar\"
   \"baz\" \"quux\"}
  ```"
  ([handler context]
    (params-normalize-wrapper handler context {}))
  ([handler context options]
    (when-wrapper-enabled ring-kdef/cfg-params-normalize-wrapper? handler context
      (let [normalize (as-> ring-kdef/cfg-params-normalize-function $
                        (opt-or-config :normalizer $)
                        (core-type/ifunc $)
                        (comp $ (fn [x] (cond
                                          (string? x) [x]
                                          (vector? x) x
                                          (coll? x)   (vec x)
                                          :otherwise  [x]))))
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
              respond raise)))))))


;; ----- unexpected to HTTP 500 -----


(defn unexpected->500-wrapper
  "Wrap given Ring handler such that if it returns unexpected Ring response (invalid/malformed response or exception)
  then return HTTP 500 Ring response.

  | Option           | Config key                             | Default config value                |
  |------------------|----------------------------------------|-------------------------------------|
  |`:on-bad-response`|`\"bract.ring.unexpected.response.fn\"` |[[bract.ring.util/bad-response->500]]|
  |`:on-exception`   |`\"bract.ring.unexpected.exception.fn\"`|[[bract.ring.util/exception->500]]   |"
  ([handler context]
    (unexpected->500-wrapper handler context {}))
  ([handler context options]
    (when-wrapper-enabled ring-kdef/cfg-unexpected->500-wrapper? handler context
      (let [;; --- bad response ---
            event-name-br   (-> context
                              core-kdef/ctx-config
                              ring-kdef/cfg-unexpected-badres-event-name)
            event-logger-br (core-kdef/resolve-event-logger context event-name-br)
            on-bad-response (->> ring-kdef/cfg-unexpected-response-fn
                              (opt-or-config :on-bad-response)
                              core-type/ifunc
                              (comp (core-util/after (event-logger-br event-name-br))))
            ;; --- exception ---
            event-name-ex   (-> context
                              core-kdef/ctx-config
                              ring-kdef/cfg-unexpected-thrown-event-name)
            event-logger-ex (core-kdef/resolve-event-logger context event-name-ex)
            on-exception    (->> ring-kdef/cfg-unexpected-exception-fn
                              (opt-or-config :on-exception)
                              core-type/ifunc
                              (comp (core-util/after (event-logger-ex event-name-ex))))
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
                  (respond (on-exception request e)))))))))))


;; ----- traffic draining -----


(defn traffic-drain-wrapper
  "Given a deref'able shutdown state and a boolean flag to respond with connection-close HTTP header, wrap specified
  Ring handler to respond with HTTP 503 (in order to drain current traffic) when the system is shutting down.

  | Option       | Config key                             | Default config value  |
  |--------------|----------------------------------------|-----------------------|
  |`:conn-close?`|`\"bract.ring.traffic.conn.close.flag\"`| `true`                |"
  ([handler context]
    (traffic-drain-wrapper handler context {}))
  ([handler context options]
    (when-wrapper-enabled ring-kdef/cfg-traffic-drain-wrapper? handler context
      (let [conn-close?   (->> ring-kdef/cfg-traffic-drain-conn-close?
                            (opt-or-config :conn-close?))
            shutdown-flag (core-kdef/*ctx-shutdown-flag context)
            response-503  (let [response {:status 503
                                          :headers {"Content-Type" "text/plain"}
                                          :body "503 Service Unavailable. Traffic draining is in progress."}]
                            (if conn-close?
                              (assoc-in response [:headers "Connection"] "close")
                              response))
            alive-tracker (core-kdef/ctx-alive-tstamp context)
            drain-respond (fn [respond response]
                            (try
                              (respond (if (and conn-close?
                                             @shutdown-flag
                                             (map? response)
                                             (integer? (:status response)))
                                         (assoc-in response [:headers "Connection"] "close")
                                         response))
                              (finally
                                ;; record last service time
                                (alive-tracker))))
            drain-handler (fn [respond invoke-handler]
                            (if @shutdown-flag
                              (respond response-503)
                              (try
                                (invoke-handler)
                                (finally
                                  ;; record last service time
                                  (alive-tracker)))))]
        ;; initialize alive-tracker
        (alive-tracker)
        ;; return wrapped handler
        (fn
          ([request]
            (drain-handler identity #(drain-respond identity (handler request))))
          ([request respond raise]
            (drain-handler respond #(handler request (partial drain-respond respond) raise))))))))


;; ----- tracing -----


(defn distributed-trace-wrapper
  "Parse distributed trace HTTP headers and populate request with well configured attributes.

  | Option                 | Config key                             | Default config value  |
  |------------------------|----------------------------------------|-----------------------|
  |`:trace-id-header`      |`\"bract.ring.trace.trace.id.header\"`  |`\"x-trace-id\"`       |
  |`:parent-id-header`     |`\"bract.ring.trace.parent.id.header\"` |`\"x-trace-parent-id\"`|
  |`:trace-id-required?`   |`\"bract.ring.trace.trace.id.req.flag\"`|`false`                |
  |`:trace-id-validator`   |`\"bract.ring.trace.trace.id.valid.fn\"`|`(constantly nil)`     |
  |`:trace-id-request-key` |`\"bract.ring.trace.trace.id.req.key\"` |`:trace-id`            |
  |`:span-id-request-key`  |`\"bract.ring.trace.span.id.req.key\"`  |`:span-id`             |
  |`:parent-id-request-key`|`\"bract.ring.trace.parent.id.req.key\"`|`:parent-id`           |"
  ([handler context]
    (distributed-trace-wrapper handler context {}))
  ([handler context options]
    (when-wrapper-enabled ring-kdef/cfg-distributed-trace-wrapper? handler context
      (let [trace-id-header       (->> ring-kdef/cfg-trace-trace-id-header
                                    (opt-or-config :trace-id-header)
                                    string/lower-case)
            parent-id-header      (->> ring-kdef/cfg-trace-parent-id-header
                                    (opt-or-config :parent-id-header)
                                    string/lower-case)
            trace-id-required?    (->> ring-kdef/cfg-trace-trace-id-required?
                                    (opt-or-config :trace-id-required?))
            trace-id-validator    (->> ring-kdef/cfg-trace-trace-id-validator
                                    (opt-or-config :trace-id-validator))
            trace-id-request-key  (->> ring-kdef/cfg-trace-trace-id-request-key
                                    (opt-or-config :trace-id-request-key))
            span-id-request-key   (->> ring-kdef/cfg-trace-span-id-request-key
                                    (opt-or-config :span-id-request-key))
            parent-id-request-key (->> ring-kdef/cfg-trace-parent-id-request-key
                                    (opt-or-config :parent-id-request-key))
            missing-trace-id      {:status 400
                                   :headers {"Content-Type" "text/plain"}
                                   :body (format "400 Missing Trace-ID

Every request must bear the header '%s'" trace-id-header)}
            invalid-trace-id-400  (fn [reason]
                                    {:status 400
                                     :headers {"Content-Type" "text/plain"}
                                     :body (format "400 Invalid Trace-ID

Header '%s' has invalid value: %s" trace-id-header reason)})
            assoc-ids             (fn [request trace-id parent-id]
                                    (let [span-id (core-util/clean-uuid)]
                                      (if trace-id
                                        (assoc request
                                          trace-id-request-key  trace-id
                                          span-id-request-key   span-id
                                          parent-id-request-key parent-id)
                                        (assoc request
                                          trace-id-request-key  span-id
                                          span-id-request-key   span-id
                                          parent-id-request-key nil))))
            handle-trace          (fn [request respond handler-more-args]
                                    (let [headers   (get request :headers)
                                          trace-id  (get headers trace-id-header)
                                          parent-id (get headers parent-id-header)]
                                      (if trace-id
                                        (if-let [error (trace-id-validator trace-id)]
                                          (respond (invalid-trace-id-400 error))
                                          (apply handler (assoc-ids request trace-id parent-id) handler-more-args))
                                        (if trace-id-required?
                                          (respond missing-trace-id)
                                          (apply handler (assoc-ids request trace-id parent-id) handler-more-args)))))]
        (fn
          ([request]
            (handle-trace request identity []))
          ([request respond raise]
            (handle-trace request respond [respond raise])))))))


;; ----- logging -----


(defn traffic-log-wrapper
  "Log traffic to the Ring handler.

  | Option            | Value type                                       | Description               | Default |
  |-------------------|--------------------------------------------------|---------------------------|---------|
  |`:request-logger`  |`(fn [request])`                                  | Request logger function   | No-op   |
  |`:response-logger` |`(fn [request response ^double duration-millis])` | Response logger function  | No-op   |
  |`:exception-logger`|`(fn [request exception ^double duration-millis])`| Exception logger function | No-op   |

  See: [[ring-mware/traffic-log-middleware]]"
  [handler context]
  (when-wrapper-enabled ring-kdef/cfg-traffic-log-wrapper? handler context
    (let [app-config (core-kdef/ctx-config context)
          {:keys [request-logger
                  response-logger
                  exception-logger]} (ring-kdef/cfg-traffic-log-wrapper-options app-config)]
      (ring-mware/traffic-log-middleware
        handler
        (cond-> {}
          request-logger   (assoc :request-logger (core-type/ifunc request-logger))
          response-logger  (assoc :response-logger (core-type/ifunc response-logger))
          exception-logger (assoc :exception-logger (core-type/ifunc exception-logger)))))))
