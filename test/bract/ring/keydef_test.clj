;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.keydef-test
  (:require
    [clojure.test :refer :all]
    [bract.ring.keydef :as kdef]))


(deftest test-context
  (testing "happy cases"
    (is (fn? (kdef/ctx-ring-handler {:bract.ring/ring-handler (fn [_])}))))
  (testing "missing/bad keys"
    (is (thrown? IllegalArgumentException (kdef/ctx-ring-handler {:bract.ring/ring-handler 10})) "invalid handler")
    (is (thrown? IllegalArgumentException (kdef/ctx-ring-handler {})) "missing handler")))
