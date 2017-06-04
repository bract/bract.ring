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
    [bract.core.config :as core-config]
    [bract.ring.config  :as ring-config]
    [bract.ring.inducer :as inducer]))


(def holder (volatile! 10))


(defn wrapper-inc
  [handler context]
  (vswap! holder (fn [^long n] (inc n)))
  handler)


(defn wrapper-add2
  [handler context]
  (vswap! holder (fn [^long n] (+ n 2)))
  handler)


(deftest test-ctx-apply-wrappers
  (testing "happy cases"
    (let [context {:bract.ring/ring-handler identity
                   :bract.ring/wrappers     [wrapper-inc wrapper-add2]}]
      (vreset! holder 0)
      (let [new-context (inducer/ctx-apply-wrappers context)
            new-handler (ring-config/ctx-ring-handler new-context)]
        (is (contains? new-context :bract.ring/wrappers))
        (is (= :foo (new-handler :foo))))
      (is (= 3 @holder))))
  (testing "missing/bad keys"
    (is (thrown? IllegalArgumentException (inducer/ctx-apply-wrappers {})))))


(deftest test-cfg-apply-wrappers
  (testing "happy cases"
    (let [context {:bract.ring/ring-handler identity
                   :bract.core/config {"bract.ring.wrappers" ['bract.ring.inducer-test/wrapper-inc
                                                              'bract.ring.inducer-test/wrapper-add2]}}]
      (vreset! holder 0)
      (let [new-context (inducer/cfg-apply-wrappers context)
            new-handler (ring-config/ctx-ring-handler new-context)]
        (is (contains? (core-config/ctx-config new-context) "bract.ring.wrappers"))
        (is (= :foo (new-handler :foo))))
      (is (= 3 @holder))))
  (testing "missing/bad keys"
    (is (thrown? IllegalArgumentException (inducer/cfg-apply-wrappers {})))))
