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
  "Start Aleph server. Include `[aleph \"version\"]` in your dependencies."
  [handler options]
  (require 'aleph.http)
  (if-let [f (find-var 'aleph.http/start-server)]
    (let [s-opts  (merge {:port 3000} options)
          ^java.io.Closeable server (f handler s-opts)
          message (format "Aleph server started on port %d" (:port s-opts))
          stopmsg (format "STOPPED Aleph server on port %d" (:port s-opts))]
      (if (Echo/isVerbose)
        (echo/echo message)
        (core-util/err-print-banner message))
      (fn aleph-stopper []
        (.close server)
        (echo/echo stopmsg)))
    (throw (ex-info "Cannot find Aleph server starter fn 'aleph.http/start-server' in classpath." {}))))


(defn start-http-kit-server
  "Start HTTP-Kit server. Include `[http-kit \"version\"]` in your dependencies."
  [handler options]
  (require 'org.httpkit.server)
  (if-let [f (find-var 'org.httpkit.server/run-server)]
    (let [s-opts  (merge {:port 3000} options)
          stopper (f handler s-opts)
          message (format "HTTP Kit server started on port %d" (:port s-opts))
          stopmsg (format "STOPPED HTTP Kit server on port %d" (:port s-opts))]
      (if (Echo/isVerbose)
        (echo/echo message)
        (core-util/err-print-banner message))
      (fn http-kit-stopper []
        (stopper)
        (echo/echo stopmsg)))
    (throw (ex-info "Cannot find HTTP-Kit server starter fn 'org.httpkit.server/run-server' in classpath." {}))))


(defn start-immutant-server
  "Start Immutant server. Include `[org.immutant/immutant \"version\"]` in your dependencies."
  [handler options]
  (require 'immutant.web)
  (if-let [f (find-var 'immutant.web/run)]
    (let [s-opts  (merge {:port 3000} options)
          server  (->> s-opts
                    seq
                    (apply concat)
                    (apply f handler))
          message (format "Immutant server started on port %d" (:port s-opts))
          stopmsg (format "STOPPED Immutant server on port %d" (:port s-opts))]
      (if (Echo/isVerbose)
        (echo/echo message)
        (core-util/err-print-banner message))
      (if-let [stopper (find-var 'immutant.web/stop)]
        (fn immutant-stopper []
          (stopper server)
          (echo/echo stopmsg))
        (throw (ex-info "Cannot find Immutant server stopper fn 'immutant.web/stop' in classpath." {}))))
    (throw (ex-info "Cannot find Immutant server starter fn 'immutant.web/run' in classpath." {}))))


(defn start-jetty-server
  "Start Jetty server using ring-jetty adapter. Include `[ring/ring-jetty-adapter \"version\"]` in your dependencies."
  [handler options]
  (require 'ring.adapter.jetty)
  (if-let [f (find-var 'ring.adapter.jetty/run-jetty)]
    (let [s-opts  (merge {:port 3000 :join? false} options)
          server  (f handler s-opts)
          message (format "Jetty server started on port %d" (:port s-opts))
          stopmsg (format "STOPPED Jetty server on port %d" (:port s-opts))]
      (if (Echo/isVerbose)
        (echo/echo message)
        (core-util/err-print-banner message))
      (import 'org.eclipse.jetty.server.Server)
      (fn jetty-stopper []
        (.stop server)
        (echo/echo stopmsg)))
    (throw (ex-info "Cannot find Jetty server starter fn 'ring.adapter.jetty/run-jetty' in classpath." {}))))


(defn start-nginx-embedded-server
  "Start nginx embedded server. Include `[nginx-clojure/nginx-clojure-embed \"version\"]` in your dependencies."
  [handler options]
  (require 'nginx.clojure.embed)
  (if-let [f (find-var 'nginx.clojure.embed/run-server)]
    (let [s-opts  (merge {:port 3000} options)
          server  (f handler s-opts)
          message (format "nginx-embedded server started on port %d" (:port s-opts))
          stopmsg (format "STOPPED nginx-embedded server on port %d" (:port s-opts))]
      (if (Echo/isVerbose)
        (echo/echo message)
        (core-util/err-print-banner message))
      (if-let [stopper (find-var 'nginx.clojure.embed/stop-server)]
        (fn nginx-embedded-stopper []
          (stopper)
          (echo/echo stopmsg))
        (throw (ex-info "Cannot find nginx-embedded server stopper fn 'nginx.clojure.embed/stop-server' in classpath."
                 {}))))
    (throw (ex-info "Cannot find nginx-embedded server starter fn 'nginx.clojure.embed/run-server' in classpath." {}))))
