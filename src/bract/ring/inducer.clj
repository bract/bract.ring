;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.inducer
  (:require
    [bract.core.inducer :as core-inducer]
    [bract.core.type    :as core-type]
    [bract.core.util    :as core-util]
    [bract.ring.keydef  :as ring-kdef]))


(defn apply-middlewares
  "Given a context with a Ring handler under context key :bract.ring/ring-handler apply the Ring middleware i.e.
  a seq of `(fn [handler & args]) -> handler`, finally updating the context with the wrapped handler."
  [context middlewares]
  (core-util/expected coll? "Ring middleware collection" middlewares)
  (->> middlewares
    (map (fn [middleware-spec] (let [[middleware-name & args] (core-util/as-vec middleware-spec)]
                                 (core-type/->Function
                                   (fn [ctx]
                                     (let [handler    (ring-kdef/ctx-ring-handler ctx)
                                           middleware (core-type/ifunc middleware-name)]
                                       (->> args
                                         (apply middleware handler)
                                         (assoc ctx (key ring-kdef/ctx-ring-handler)))))
                                   (core-type/iname middleware-name)
                                   []))))
    (core-inducer/induce context)))


(defn apply-wrappers
  "Given a context with a Ring handler under context key :bract.ring/ring-handler apply the Ring handler wrappers i.e.
  a seq of `(fn [handler context]) -> handler`, finally updating the context with the wrapped handler."
  [context wrappers]
  (core-util/expected coll? "Ring handler wrapper collection" wrappers)
  (->> wrappers
    (map (fn [wrapper-spec] (let [[wrapper-name & args] (core-util/as-vec wrapper-spec)]
                              (core-type/->Function
                                (fn [ctx]
                                  (let [handler (ring-kdef/ctx-ring-handler ctx)
                                        wrapper (core-type/ifunc wrapper-name)]
                                    (->> args
                                      (apply wrapper handler ctx)
                                      (assoc ctx (key ring-kdef/ctx-ring-handler)))))
                                (core-type/iname wrapper-name)
                                []))))
    (core-inducer/induce context)))
