;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.inducer
  (:require
    [bract.core.config  :as core-config]
    [bract.core.inducer :as core-inducer]
    [bract.core.type    :as core-type]
    [bract.core.util    :as core-util]
    [bract.ring.config  :as ring-config]))


(defn apply-wrappers
  "Given a context with Ring handler apply the Ring handler wrappers i.e. a seq of `(fn [handler context]) -> handler`,
  finally updating the context with the handler."
  [context wrappers]
  (core-util/expected coll? "Ring handler wrapper collection" wrappers)
  (->> wrappers
    (map (fn [wrapper-spec] (core-type/->Function
                              (fn [ctx]
                                (let [handler (ring-config/ctx-ring-handler ctx)
                                      wrapper (core-type/ifunc wrapper-spec)]
                                  (->> (wrapper handler ctx)
                                    (assoc ctx (key ring-config/ctx-ring-handler)))))
                              (core-type/iname wrapper-spec)
                              [])))
    (core-inducer/induce context)))


(defn ctx-apply-wrappers
  "Given a context with Ring handler and Ring handler wrappers (under the context key :bract.ring/wrappers), i.e. a
  seq of `(fn [handler context]) -> handler`, apply them in order finally updating the context with the handler."
  [context]
  (ring-config/apply-wrappers
    context
    (key ring-config/cfg-wrappers)
    (ring-config/ctx-wrappers context)))


(defn cfg-apply-wrappers
  "Given a context with Ring handler and Ring handler wrappers (under the config key \"bract.ring.wrappers\"), i.e. a
  seq of `(fn [handler context]) -> handler`, apply them in order finally updating the context with the handler."
  [context]
  (ring-config/apply-wrappers
    context
    (key ring-config/cfg-wrappers)
    (-> context core-config/ctx-config ring-config/cfg-wrappers)))
