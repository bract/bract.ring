;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.handler
  (:require
    [clojure.pprint :as pp]
    [bract.core.util.runtime :as bcu-runtime]))


(defn info-edn-handler
  ([]
    {:status 200
     :body (-> (bcu-runtime/sysinfo)
             pp/pprint
             with-out-str)
     :headers {"Content-Type"  "application/edn"
               "Cache-Control" "no-store, no-cache, must-revalidate"}})
  ([request] (info-edn-handler))
  ([request respond raise] (respond (info-edn-handler))))


(defn make-info-json-handler
  [json-encoder]
  (fn info-json
    ([]
      {:status 200
       :body (json-encoder (bcu-runtime/sysinfo))
       :headers {"Content-Type"  "application/json"
                 "Cache-Control" "no-store, no-cache, must-revalidate"}})
    ([request] (info-json))
    ([request respond raise] (respond (info-json)))))
