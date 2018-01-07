;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.wrapper
  (:require
    [bract.ring.handler :as br-handler]))


(defn info-edn-wrapper
  ([handler context]
    (info-edn-wrapper handler context #{"/info/edn" "/info/edn/"}))
  ([handler context info-uris]
    (let [info-uris-set (set info-uris)]
      (fn info-edn
        ([request]
          (if (->> (:uri request)
                (contains? info-uris-set))
            (br-handler/info-edn-handler)
            (handler request)))
        ([request respond raise]
          (if (->> (:uri request)
                (contains? info-uris-set))
            (respond (br-handler/info-edn-handler))
            (handler request respond raise)))))))


(defn info-json-wrapper
  ([handler context json-encoder]
    (info-json-wrapper handler context json-encoder #{"/info/json" "/info/json/"}))
  ([handler context json-encoder info-uris]
    (let [info-uris-set (set info-uris)
          gen-response  (br-handler/make-info-json-handler json-encoder)]
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


(defn ping-wrapper
  ([handler context]
    (ping-wrapper handler context #{"/ping" "/ping/"}))
  ([handler context ping-uris]
    (ping-wrapper handler context ping-uris (constantly "pong")))
  ([handler context ping-uris body-generator]
    (let [ping-uris-set (set ping-uris)
          ping-response (fn [] {:status 200
                                :body (body-generator)
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
