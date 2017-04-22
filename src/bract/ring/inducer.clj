;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.inducer
  (:require
    [bract.core.config :as bc-config]
    [bract.core.echo   :as bc-echo]
    [bract.core.util   :as bc-util]
    [bract.ring.config :as config]))


(defn apply-wrappers
  "Given a context, look up the ring-handler inducers and apply them successively returning an updated context."
  [context]
  (bc-echo/echo "Applying Ring-handler inducers")
  (config/ctx-ring-handler context)  ; assert the handler is present
  (->> (bc-config/ctx-config context)
    config/cfg-wrappers
    (bc-util/induce context (partial bc-config/apply-inducer-by-name
                              "Ring-handler inducer" (key config/cfg-wrappers)))))
