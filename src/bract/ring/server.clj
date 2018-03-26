;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.server
  "This namespace defines functions that start popular Ring servers, returning stopper function. Server dependencies
  are not included - you must include them in the project."
  (:require
    [bract.core.echo :as echo]
    [bract.core.util :as core-util])
  (:import
    [java.io Closeable]
    [bract.core Echo]))


(defn start-aleph-server
  "Start Aleph server. Include [aleph \"version\"] in your dependencies."
  [handler options]
  (require 'aleph.http)
  (if-let [f (find-var 'aleph.http/start-server)]
    (let [s-opts  (merge {:port 3000} options)
          ^java.io.Closeable server (f handler s-opts)
          message (format "Aleph server started on port %d" (:port s-opts))]
      (if (Echo/isVerbose)
        (echo/echo message)
        (core-util/err-println message))
      (fn [] (.close server)))
    (throw (ex-info "Cannot find Aleph server starter fn 'aleph.http/start-server' in classpath." {}))))


(defn start-http-kit-server
  "Start HTTP-Kit server. Include [http-kit \"version\"] in your dependencies."
  [handler options]
  (require 'org.httpkit.server)
  (let [s-opts  (merge {:port 3000} options)
        message (format "HTTP Kit server started on port %d" (:port s-opts))]
    (-> (find-var 'org.httpkit.server/run-server)
      (or (throw (ex-info "Cannot find HTTP-Kit server starter fn 'org.httpkit.server/run-server' in classpath." {})))
      (core-util/invoke handler s-opts)
      (doto (do (if (Echo/isVerbose)
                  (echo/echo message)
                  (core-util/err-println message)))))))


(defn start-immutant-server
  "Start Jetty server using ring-jetty adapter. Include [org.immutant/immutant \"version\"] in your dependencies."
  [handler options]
  (require 'immutant.web)
  (if-let [f (find-var 'immutant.web/run)]
    (let [s-opts  (merge {:port 3000} options)
          server  (->> s-opts
                    seq
                    (apply concat)
                    (apply f handler))
          message (format "Immutant server started on port %d" (:port s-opts))]
      (if (Echo/isVerbose)
        (echo/echo message)
        (core-util/err-println message))
      (if-let [stopper (find-var 'immutant.web/stop)]
        (fn [] (stopper server))
        (throw (ex-info "Cannot find Immutant server stopper fn 'immutant.web/stop' in classpath." {}))))
    (throw (ex-info "Cannot find Immutant server starter fn 'immutant.web/run' in classpath." {}))))


(defn start-jetty-server
  "Start Jetty server using ring-jetty adapter. Include [ring/ring-jetty-adapter \"version\"] in your dependencies."
  [handler options]
  (require 'ring.adapter.jetty)
  (if-let [f (find-var 'ring.adapter.jetty/run-jetty)]
    (let [s-opts  (merge {:port 3000 :join? false} options)
          server  (f handler s-opts)
          message (format "Jetty server started on port %d" (:port s-opts))]
      (if (Echo/isVerbose)
        (echo/echo message)
        (core-util/err-println message))
      (import 'org.eclipse.jetty.server.Server)
      (fn [] (.stop server)))
    (throw (ex-info "Cannot find Jetty server starter fn 'ring.adapter.jetty/run-jetty' in classpath." {}))))
