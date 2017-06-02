;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.dev
  (:require
    [bract.core.dev    :as core-dev]
    [bract.core.echo   :as core-echo]
    [bract.ring.config :as ring-config]))


(defn- unprepared-handler
  [request]
  (throw (ex-info "Ring handler is not yet initialized" {})))


(def ^:redef handler unprepared-handler)


(defn init!
  "Initialize environment and the Ring handler."
  ([]
    (init! (core-dev/init)))
  ([context]
    (let [ctx-handler (ring-config/ctx-ring-handler context)]
      (alter-var-root #'handler (constantly ctx-handler))
      (core-echo/echo "Updated bract.ring.dev/handler")
      context)))


(defn init-once!
  "Given a var e.g. (defonce a-var nil) having logical false value, set it to `true` and initialize app in DEV mode,
  finally updating the bract.ring.dev/handler var."
  ([]
    (when-let [context (core-dev/init-once!)]
      (init! context)))
  ([a-var]
    (when-let [context (core-dev/init-once! a-var)]
      (init! context))))
