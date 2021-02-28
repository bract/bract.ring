;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.middleware-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [bract.ring.middleware :as m])
  (:import
    [clojure.lang ExceptionInfo IDeref IFn]))


(deftest test-traffic-log-middleware
  (let [called (fn []
                 (let [a (atom {:count 0
                                :arity -1})
                       t (fn [n]
                           (swap! a (fn [old]
                                      (-> old
                                        (update :count inc)
                                        (assoc :arity n)))))]
                   (reify
                     IDeref
                     (deref  [_]       @a)
                     IFn
                     (invoke [_]       (t 0))
                     (invoke [_ _]     (t 1))
                     (invoke [_ _ _]   (t 2))
                     (invoke [_ _ _ _] (t 3)))))]
    (testing "happy case - no exception"
      (let [request-logger  (called)
            response-logger (called)
            ex-logger       (called)
            handler (fn [request] {:status 200
                                   :body "OK"})
            wrapped (-> handler
                      (m/traffic-log-middleware
                        {:request-logger (fn [req] (request-logger req))
                         :response-logger (fn [req res ^double dur] (response-logger req res dur))
                         :exception-logger (fn [req res ^double dur] (ex-logger req res dur))}))]
        (is (= {:status 200
                :body "OK"}
              (wrapped {:uri "/foo" :request-method :get})))
        (is (= {:count 1
                :arity 1}
              @request-logger))
        (is (= {:count 1
                :arity 3}
              @response-logger))
        (is (= {:count 0
                :arity -1}
              @ex-logger))))
    (testing "unhappy case - exception thrown"
      (let [request-logger (called)
            response-logger (called)
            ex-logger (called)
            handler (fn [request] (throw (ex-info "processing error" {:reasone :test})))
        wrapped (-> handler
                  (m/traffic-log-middleware {:request-logger (fn [req] (request-logger req))
                                             :response-logger (fn [req res ^double dur] (response-logger req res dur))
                                             :exception-logger (fn [req res ^double dur] (ex-logger req res dur))}))]
        (is (thrown? ExceptionInfo
              (wrapped {:uri "/foo" :request-method :get})))
        (is (= {:count 1
                :arity 1}
              @request-logger))
        (is (= {:count 0
                :arity -1}
              @response-logger))
        (is (= {:count 1
                :arity 3}
              @ex-logger))))))
