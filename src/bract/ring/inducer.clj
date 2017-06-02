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


(defn apply-wrappers-with-context
  "Given a context with Ring handler, look up the ring-handler wrappers `(fn [handler context]) -> handler` and apply
  them successively to the handler, finally updating the context with the handler before returning it."
  ([context]
    (apply-wrappers-with-context
      "Ring wrapper (fn [handler context])" config/cfg-wrappers-context identity context))
  ([wrapper-type config-def f context]
    (bc-echo/echo "Applying Ring-middleware")
    (let [handler  (config/ctx-ring-handler context)]
      (->> (bc-config/ctx-config context)
        config-def
        (bc-util/induce handler (fn [updated-handler wrapper-name]
                                  (config/apply-wrapper-by-name
                                    wrapper-type
                                    (key config-def)
                                    updated-handler
                                    (f context)
                                    wrapper-name)))
        (assoc context (key config/ctx-ring-handler))))))


(defn apply-wrappers-with-config
  "Given a context with Ring handler, look up the ring-handler wrappers `(fn [handler config]) -> handler` and apply
  them successively to the handler, finally updating the context with the handler before returning it."
  [context]
  (apply-wrappers-with-context
    "Ring wrapper (fn [handler config])" config/cfg-wrappers-config bc-config/ctx-config context))


(defn ctx-apply-wrappers
  "Given a context with wrappers under the key :bract.ring/wrappers, i.e. a seq of `(fn [handler context]) -> handler`,
  apply them in a sequence."
  [context]
  (config/apply-wrappers
    context
    (key config/cfg-wrappers)
    (config/ctx-wrappers context)))


(defn cfg-apply-wrappers
  "Given a context having config with wrappers under the config key \"bract.ring.wrappers\", i.e. a seq of
  `(fn [handler context]) -> handler`, apply them in a sequence."
  [context]
  (config/apply-wrappers
    context
    (key config/cfg-wrappers)
    (-> context bc-config/ctx-config config/cfg-wrappers)))
