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
