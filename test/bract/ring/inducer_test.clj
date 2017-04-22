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
    [bract.core.config :as bc-config]
    [bract.ring.inducer :as inducer]))


(defonce sample-value (volatile! 10))


(defn sample-wrapper
  [handler context]
  (vswap! sample-value (fn [^long n] (+ n 10))))


(deftest test-apply-wrappers-context
  (testing "happy cases"
    (let [good-context {:bract.ring/ring-handler identity
                        :bract.core/config {"bract.ring.wrappers.context" ['bract.ring.inducer-test/sample-wrapper]}}]
      (vreset! sample-value 10)
      (is (= 10 @sample-value))
      (let [context (inducer/apply-wrappers-with-context good-context)]
        (is (contains? context :bract.ring/ring-handler))
        (is (contains? context :bract.core/config)))
      (is (= 20 @sample-value))))
  (testing "missing/bad keys"
    (is (thrown? IllegalArgumentException (inducer/apply-wrappers-with-context {})))))


(deftest test-apply-wrappers-config
  (testing "happy cases"
    (let [good-context {:bract.ring/ring-handler identity
                        :bract.core/config {"bract.ring.wrappers.config" ['bract.ring.inducer-test/sample-wrapper]}}]
      (vreset! sample-value 10)
      (is (= 10 @sample-value))
      (let [context (inducer/apply-wrappers-with-config good-context)]
        (is (contains? context :bract.ring/ring-handler))
        (is (contains? context :bract.core/config)))
      (is (= 20 @sample-value))))
  (testing "missing/bad keys"
    (is (thrown? IllegalArgumentException (inducer/apply-wrappers-with-config {})))))
