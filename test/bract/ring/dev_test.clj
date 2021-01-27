;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.dev-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [bract.ring.dev :as ring-dev]))


(deftest test-log-request
  (doseq [request [{:request-method :get  :uri "/items"}
                   {:request-method :post :uri "/items" :headers {"content-type" "application/json"}}
                   {:request-method :put  :uri "/item/123" :headers {"content-type" "application/json"
                                                                     "authorization" "Bearer 1234567890"}}]]
    (ring-dev/log-request request)))


(deftest test-log-response
  (doseq [[request
           response] [[{:request-method :get  :uri "/items"} {:status 200
                                                              :headers {"Content-Type" "application/json"}
                                                              :body "[{\"name\": \"O Ring\", \"unit\": \"each\"}]"}]
                      [{:request-method :post :uri "/items"} {:status 201
                                                              :headers {"Location" "http://myapp.com/item/12"}}]
                      [{:request-method :put  :uri "/item/1"} {:status 401
                                                               :headers {"Content-Type" "text/plain"}
                                                               :body "Not authorized to update item"}]
                      ;; non-map response
                      [{:request-method :get  :uri "/foo"}    "Bad request"]
                      [{:request-method :post :uri "/bar"}    39993]]]
    (ring-dev/log-response request response (* ^long (rand-int 50) ^double (rand)))))


(deftest test-log-zexception
  (doseq [[request exception] [[{:request-method :get    :uri "/items"}      (Exception. "test")]
                               [{:request-method :delete :uri "/items/1234"} (RuntimeException. "shelf life expired")]
                               [{:request-method :post   :uri "/foo"}        (Error. "No such resource")]]]
    (ring-dev/log-exception request exception (* ^long (rand-int 50) ^double (rand)))))
