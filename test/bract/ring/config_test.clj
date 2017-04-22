;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.config-test
  (:require
    [clojure.test :refer :all]
    [bract.ring.config :as config]))


(deftest test-context
  (testing "happy cases"
    (is (fn? (config/ctx-ring-handler {:bract.ring/ring-handler (fn [_])}))))
  (testing "missing/bad keys"
    (is (thrown? IllegalArgumentException (config/ctx-ring-handler {:bract.ring/ring-handler 10})) "invalid")
    (is (thrown? IllegalArgumentException (config/ctx-ring-handler {})) "missing")))


(deftest test-config
  (testing "happy cases"
    (is (vector? (config/cfg-wrappers-context {"bract.ring.wrappers.context" ["foo.bar/baz"
                                                                              "foo.bar/qux"]})))
    (is (vector? (config/cfg-wrappers-config  {"bract.ring.wrappers.config"  ["foo.bar/baz"
                                                                              "foo.bar/qux"]}))))
  (testing "missing/bad keys"
    (is (thrown? IllegalArgumentException (config/cfg-wrappers-context {"bract.ring.wrappers.context" 20})) "invalid")
    (is (thrown? IllegalArgumentException (config/cfg-wrappers-context {})) "missing")
    (is (thrown? IllegalArgumentException (config/cfg-wrappers-config  {"bract.ring.wrappers.config"  20})) "invalid")
    (is (thrown? IllegalArgumentException (config/cfg-wrappers-config  {})) "missing")))
