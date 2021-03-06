;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.inducer-test
  (:require
    [clojure.test :refer :all]
    [bract.core.inducer :as core-inducer]
    [bract.ring.keydef  :as ring-kdef]
    [bract.ring.inducer :as inducer]
    [bract.ring.server-test :as stest]))


(def holder (volatile! 10))


(defn middleware-inc
  [handler]
  (vswap! holder (fn [^long n] (inc n)))
  handler)


(defn middleware-add
  [handler ^long delta]
  (vswap! holder (fn [^long n] (+ n delta)))
  handler)


(deftest test-apply-middleware
  (testing "happy cases, wrapper fns"
    (let [context {:bract.ring/ring-handler identity}]
      (vreset! holder 0)
      (let [new-context (inducer/apply-middlewares context [middleware-inc
                                                            [middleware-add 2]])
            new-handler (ring-kdef/ctx-ring-handler new-context)]
        (is (contains? new-context :bract.ring/ring-handler))
        (is (= :foo (new-handler :foo))))
      (is (= 3 @holder))))
  (testing "happy cases, wrapper names"
    (let [context {:bract.ring/ring-handler identity}]
      (vreset! holder 0)
      (let [new-context (inducer/apply-middlewares context '[bract.ring.inducer-test/middleware-inc
                                                             [bract.ring.inducer-test/middleware-add 2]])
            new-handler (ring-kdef/ctx-ring-handler new-context)]
        (is (contains? new-context :bract.ring/ring-handler))
        (is (= :foo (new-handler :foo))))
      (is (= 3 @holder))))
  (testing "empty middlewares collection"
    (inducer/apply-middlewares {} []))
  (testing "missing Ring handler"
    (is (thrown? IllegalArgumentException (inducer/apply-middlewares {} [(fn [x y] x)])))))


(defn wrapper-inc
  [handler context]
  (vswap! holder (fn [^long n] (inc n)))
  handler)


(defn wrapper-add
  [handler context ^long delta]
  (vswap! holder (fn [^long n] (+ n delta)))
  handler)


(deftest test-apply-wrappers
  (testing "happy cases, wrapper fns"
    (let [context {:bract.ring/ring-handler identity}]
      (vreset! holder 0)
      (let [new-context (inducer/apply-wrappers context [wrapper-inc
                                                         [wrapper-add 2]])
            new-handler (ring-kdef/ctx-ring-handler new-context)]
        (is (contains? new-context :bract.ring/ring-handler))
        (is (= :foo (new-handler :foo))))
      (is (= 3 @holder))))
  (testing "happy cases, wrapper names"
    (let [context {:bract.ring/ring-handler identity}]
      (vreset! holder 0)
      (let [new-context (inducer/apply-wrappers context '[bract.ring.inducer-test/wrapper-inc
                                                          [bract.ring.inducer-test/wrapper-add 2]])
            new-handler (ring-kdef/ctx-ring-handler new-context)]
        (is (contains? new-context :bract.ring/ring-handler))
        (is (= :foo (new-handler :foo))))
      (is (= 3 @holder))))
  (testing "empty wrappers collection"
    (inducer/apply-wrappers {} []))
  (testing "missing Ring handler"
    (is (thrown? IllegalArgumentException (inducer/apply-wrappers {} [(fn [x y] x)])))))


(deftest test-start-server
  (doseq [each-starter ['bract.ring.server/start-aleph-server
                        'bract.ring.server/start-http-kit-server
                        'bract.ring.server/start-jetty-server]
          context-delta [{:bract.ring/server-options {:port 3000
                                                      :join? false}}
                         {:bract.core/config {"bract.ring.server.options" {:port 3000
                                                                           :join? false}}}]]
    (testing "context options")
    (testing "context options")
    (let [context (merge {:bract.ring/ring-handler   stest/handler
                          :bract.ring/server-starter each-starter}
                    context-delta)
          updated (inducer/start-server context)]
      (is (= {:headers {}
              :status 200
              :body "OK"}
            (stest/client-get)))
      (core-inducer/invoke-stopper updated))))
