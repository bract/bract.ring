;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.server-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [clj-http.client   :as client]
    [bract.ring.server :as server]))


(defn handler
  ([request]
    {:status 200
     :body "OK"})
  ([request respond raise]
    (respond (handler request))))


(defn client-get
  []
  (let [path "/"
        http-options {}]
    (-> (str "http://localhost:3000" path)
      (client/get (merge {:throw-exceptions false} http-options))
      (dissoc
        :trace-redirects :length :orig-content-encoding
        :request-time :repeatable? :protocol-version
        :streaming? :chunked? :reason-phrase)
      (update-in [:headers] dissoc "Date" "Connection" "Server" "Content-Length" "content-length"))))


(deftest test-aleph
  (let [stopper (server/start-aleph-server handler {:port 3000})]
    (is (= {:headers {} :status 200 :body "OK"}
          (client-get)))
    (stopper)))


(deftest test-http-kit
  (let [stopper (server/start-http-kit-server handler {:port 3000})]
    (is (= {:headers {} :status 200 :body "OK"}
          (client-get)))
    (stopper)))


(deftest test-immutant
  (let [stopper (server/start-immutant-server handler {:port 3000})]
    (is (= {:headers {} :status 200 :body "OK"}
          (client-get)))
    (stopper)))


(deftest test-jetty
  (let [stopper (server/start-jetty-server handler {:port 3000
                                                    :join? false})]
    (is (= {:headers {} :status 200 :body "OK"}
          (client-get)))
    (stopper)))
