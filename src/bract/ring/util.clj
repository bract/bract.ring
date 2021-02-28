;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.util
  "Utility functions."
  (:require
    [bract.core.util :as core-util]))


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


(defn elapsed-millis
  "Return elapsed milliseconds (with decimal places) since start time in nanoseconds."
  ^double [^long start-nanos]
  (let [total-ns (unchecked-subtract (System/nanoTime) start-nanos)]
    (double (/ total-ns 1e6))))
