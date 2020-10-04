;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.middleware
  "Ring middleware functions and helpers."
  (:require
    [bract.ring.util :as util]))


(defn traffic-log-middleware
  "Ring middleware that logs request/response/exception using corresponding callback handlers. Options:

  | Kwarg             | Value type                                       | Description               | Default |
  |-------------------|--------------------------------------------------|---------------------------|---------|
  |`:request-logger`  |`(fn [request])`                                  | Request logger function   | No-op   |
  |`:response-logger` |`(fn [request response ^double duration-millis])` | Response logger function  | No-op   |
  |`:exception-logger`|`(fn [request exception ^double duration-millis])`| Exception logger function | No-op   |"
  ([handler]
    (traffic-log-middleware handler {}))
  ([handler {:keys [request-logger
                    response-logger
                    exception-logger]
             :or {request-logger (fn [request])
                  response-logger (fn [request response ^double duration-millis])
                  exception-logger (fn [request exception ^double duration-millis])}
             :as options}]
    (fn logging-handler
      ([request]
        (request-logger request)
        (let [start-nanos (System/nanoTime)]
          (try
            (let [response (handler request)]
              (response-logger request response (util/elapsed-millis start-nanos))
              response)
            (catch Throwable e
              (exception-logger request e (util/elapsed-millis start-nanos))
              (throw e)))))
      ([request respond raise]
        (request-logger request)
        (let [start-nanos (System/nanoTime)
              vresponse (volatile! nil)
              vcaught   (volatile! nil)]
          (try
            (handler request #(vreset! vresponse %) #(vreset! vcaught %))
            (catch Throwable e
              (exception-logger request e (util/elapsed-millis start-nanos))
              (throw e)))
          (let [duration (util/elapsed-millis start-nanos)]
            (if-some [caught @vcaught]
              (do
                (exception-logger request caught duration)
                (raise caught))
              (let [response @vresponse]
                (response-logger request response duration)
                (respond response)))))))))
