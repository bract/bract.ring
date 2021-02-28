;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.inducer
  "Inducers provided in the bract.ring module."
  (:require
    [bract.core.echo    :as echo]
    [bract.core.inducer :as core-inducer]
    [bract.core.keydef  :as core-kdef]
    [bract.core.type    :as core-type]
    [bract.core.util    :as core-util]
    [bract.ring.keydef  :as ring-kdef]))


(defn apply-middlewares
  "Given a context with a Ring handler under context key `:bract.ring/ring-handler` apply the Ring middleware i.e.
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
  "Given a context with a Ring handler under context key `:bract.ring/ring-handler` apply the Ring handler wrappers i.e.
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


(defn start-server
  "Given a context with a Ring handler under context key `:bract.ring/ring-handler` start a web server (specified as
  argument or under the context key `:bract.ring/server-starter`) using server options available under context key
  `:bract.ring/server-options` and config key `\"bract.ring.server.options\"`."
  ([context]
    (start-server context
      (ring-kdef/ctx-server-starter context)
      (ring-kdef/ctx-server-stopper context)))
  ([context starter-fn]
    (start-server context
      starter-fn
      (ring-kdef/ctx-server-stopper context)))
  ([context starter-fn stopper-inducer]
    (let [server-func  (core-type/ifunc starter-fn)
          server-name  (core-type/iname starter-fn)
          server-args  (core-type/iargs starter-fn)
          stopper-func (core-type/ifunc stopper-inducer)
          stopper-name (core-type/iname stopper-inducer)
          stopper-args (core-type/iargs stopper-inducer)]
      (core-util/expected empty? (format "no specified arguments for server-starter fn '%s'" server-name) server-args)
      (core-util/expected empty? (format "no specified arguments for stopper-helper fn '%s'" stopper-name) stopper-args)
      (let [handler (ring-kdef/ctx-ring-handler context)
            ctx-key (key ring-kdef/ctx-server-options)
            cfg-key (key ring-kdef/cfg-server-options)
            options (if (contains? context ctx-key)
                      (do
                        (-> (format "Retrieving Ring server options for '%s' from context at key:" server-name)
                          (echo/echo ctx-key))
                        (-> (ring-kdef/ctx-server-options context)
                          (echo/->echo (format "Starting Ring server using '%s' and context options" server-name))))
                      (do
                        (-> (format "Retrieving Ring server options for '%s' from config at key:" server-name)
                          (echo/echo cfg-key))
                        (-> (core-kdef/ctx-config context)
                          ring-kdef/cfg-server-options
                          (echo/->echo (format "Starting Ring server using '%s' and config options" server-name)))))
            stopper (server-func handler options)]
        (stopper-func context stopper)))))
