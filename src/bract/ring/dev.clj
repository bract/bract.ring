;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.dev
  (:require
    [bract.core.dev    :as bc-dev]
    [bract.core.echo   :as bc-echo]
    [bract.ring.config :as config]))


(defn- unprepared-handler
  [request]
  (throw (ex-info "Ring handler is not yet initialized" {})))


(def ^:redef handler unprepared-handler)


(defn init
  "Initialize environment and the Ring handler."
  ([]
    (init (bc-dev/init)))
  ([context]
    (let [ctx-handler (config/ctx-ring-handler context)]
      (alter-var-root #'handler (constantly ctx-handler))
      (bc-echo/echo "Updated bract.ring.dev/handler")
      context)))
