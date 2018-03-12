;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.server
  "This namespace defines functions that start popular Ring servers, returning stopper function. Server dependencies
  are not included - you must include them in the project.")


(defn start-jetty-server
  "Start Jetty server using ring-jetty adapter. Include [ring/ring-jetty-adapter \"version\"] in your dependencies."
  [handler options]
  (let [form `(do
                (require 'ring.adapter.jetty)
                (import 'org.eclipse.jetty.server.Server)
                (let [^org.eclipse.jetty.server.Server server# (ring.adapter.jetty/run-jetty
                                                                 ~handler ~options)]
                  (fn [] (.stop server#))))]
    (eval form)))


(defn start-aleph-server
  "Start Aleph server. Include [aleph \"version\"] in your dependencies."
  [handler options]
  (let [form `(do
                (require 'aleph.http)
                (import 'java.io.Closeable)
                (let [^java.io.Closeable server# (aleph.http/start-server ~handler ~options)]
                  (fn [] (.close server#))))]
    (eval form)))


(defn start-http-kit-server
  "Start HTTP-Kit server. Include [http-kit \"version\"] in your dependencies."
  [handler options]
  (let [form `(do
                (require 'org.httpkit.server)
                (org.httpkit.server/run-server ~handler ~options))]
    (eval form)))
