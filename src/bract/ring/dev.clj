;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.dev
  "Development support for bract.ring module."
  (:require
    [clojure.stacktrace :as st]
    [bract.core.dev    :as core-dev]
    [bract.core.echo   :as core-echo]
    [bract.core.util   :as core-util]
    [bract.ring.keydef :as ring-kdef]))


;; ----- lein-ring support -----


(defn- unprepared-handler
  [request]
  (throw (ex-info "Ring handler is not yet initialized" {})))


(def ^:redef handler unprepared-handler)


(defn init!
  "Initialize environment and the Ring handler."
  ([]
    (init! (core-dev/init)))
  ([context]
    (let [ctx-handler (ring-kdef/ctx-ring-handler context)]
      (alter-var-root #'handler (constantly ctx-handler))
      (core-echo/echo "Updated bract.ring.dev/handler")
      context)))


(defn init-once!
  "Given a var e.g. `(defonce a-var nil)` having logical false value, set it to `true` and initialize app in DEV mode,
  finally updating the [[bract.ring.dev/handler]] var."
  ([]
    (when-let [context (core-dev/init-once!)]
      (init! context)))
  ([a-var]
    (when-let [context (core-dev/init-once! a-var)]
      (init! context))))


;; ----- Ring traffic logging support -----


(def http-method
  {:delete  "DELETE"
   :get     "GET"
   :head    "HEAD"
   :options "OPTIONS"
   :patch   "PATH"
   :post    "POST"
   :put     "PUT"})


(def http-status
  {;; 1xx
   100 "Continue"
   101 "Switching protocol"
   102 "Processing (WebDAV)"
   103 "Early Hints"
   ;; 2xx
   200 "OK"
   201 "Created"
   202 "Accepted"
   203 "Non-Authoritative Information"
   204 "No Content"
   205 "Reset Content"
   206 "Partial Content"
   207 "Multi-Status (WebDAV)"
   208 "Already Reported (WebDAV)"
   226 "IM Used (HTTP Delta Encoding)"
   ;; 3xx
   300 "Multiple Choice"
   301 "Moved Permanently"
   302 "Found"
   303 "See Other"
   304 "Not Modified"
   305 "Use Proxy"
   306 "Switch Proxy"
   307 "Temporary Redirect"
   308 "Permanent Redirect"
   ;; 4xx
   400 "Bad Request"
   401 "Unauthorized"
   402 "Payment Required"
   403 "Forbidden"
   404 "Not Found"
   405 "Method Not Allowed"
   406 "Not Acceptable"
   407 "Proxy Authentication Required"
   408 "Request Timeout"
   409 "Conflict"
   410 "Gone"
   411 "Length Required"
   412 "Precondition Failed"
   413 "Payload Too Large"
   414 "URI Too Long"
   415 "Unsupported Media Type"
   416 "Range Not Satisfiable"
   417 "Expectation Failed"
   418 "I'm a teapot"
   421 "Misdirected Request"
   422 "Unprocessable Entity (WebDAV)"
   423 "Locked (WebDAV)"
   424 "Failed Dependency (WebDAV)"
   425 "Too Early"
   426 "Upgrade Required"
   428 "Precondition Required"
   429 "Too Many Requests"
   431 "Request Header Fields Too Large"
   451 "Unavailable For Legal Reasons"
   ;; 5xx
   500 "Internal Server Error"
   501 "Not Implemented"
   502 "Bad Gateway"
   503 "Service Unavailable"
   504 "Gateway Timeout"
   505 "HTTP Version Not Supported"
   506 "Variant Also Negotiates"
   507 "Insufficient Storage (WebDAV)"
   508 "Loop Detected (WebDAV)"
   510 "Not Extended"
   511 "Network Authentication Required"
   })


(defn log-request
  "Log Ring request.
  See: `bract.ring.middleware/traffic-log-middleware`, `bract.ring.wrapper/traffic-log-wrapper`"
  [request]
  (let [{:keys [request-method
                uri
                headers]} request]
    (-> "%7s %s | %s"
      (format (http-method request-method) uri (pr-str headers))
      core-util/err-println)))


(defn log-outcome
  "Common function to log response and exception.
  See: [[log-response]], [[log-exception]]"
  [request outcome-string ^double duration-millis]
  (let [{:keys [request-method
                uri
                headers]} request
        [request-methstr
         request-uri
         request-headers] [(http-method request-method) uri (pr-str headers)]]
    (-> "%10.2fms | %-60s | %7s %s %s"
      (format duration-millis outcome-string
        request-methstr request-uri request-headers)
      core-util/err-println)))


(defn log-response
  "Log Ring response.
  See: `bract.ring.middleware/traffic-log-middleware`, `bract.ring.wrapper/traffic-log-wrapper`"
  [request response ^double duration-millis]
  (log-outcome
    request
    (if (map? response)
      (let [{:keys [status
                    headers]} response
            [response-status response-statusmsg response-headers] [status (http-status status) (pr-str headers)]]
        (format "%d %-15s %-40s" response-status response-statusmsg response-headers))
      (format "Bad response: (%s) %s" (.getName (class response)) (pr-str response)))
    duration-millis))


(defn log-exception
  "Log Ring request processing exception.
  See: `bract.ring.middleware/traffic-log-middleware`, `bract.ring.wrapper/traffic-log-wrapper`"
  [request exception ^double duration-millis]
  (log-outcome
    request
    (format "%s: %s" (.getName ^Class (class exception)) (.getMessage ^Throwable exception))
    duration-millis)
  (st/print-stack-trace exception))
