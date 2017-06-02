;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.config
  (:require
    [keypin.core :as keypin]
    [keypin.util :as kputil]
    [bract.core.echo :as echo]
    [bract.core.util :as util]))


(keypin/defkey  ; context keys
  ctx-ring-handler [:bract.ring/ring-handler fn?     "Application Ring handler"]
  ctx-wrappers     [:bract.ring/wrappers     vector? "Wrapper fns or their fully qualified names"])


(keypin/defkey  ; config keys
  cfg-wrappers     ["bract.ring.wrappers"    vector? "Fully qualified wrapper fn names" {:parser kputil/any->edn}]
  cfg-wrappers-context ["bract.ring.wrappers.context" vector? "Fully qualified wrapper (by context) fn names"
                        {:parser kputil/any->edn}]
  cfg-wrappers-config  ["bract.ring.wrappers.config"  vector? "Fully qualified wrapper (by config) fn names"
                        {:parser kputil/any->edn}])


(defn apply-wrapper-by-name
  [wrapper-type config-key handler context wrapper-name]
  (echo/echo (format "Looking up %s '%s'" wrapper-type wrapper-name))
  (let [f (kputil/str->var->deref config-key wrapper-name)]
    (echo/echo (format "Executing  %s '%s'" wrapper-type wrapper-name))
    (echo/with-inducer-name wrapper-name
      (f handler context))))


(defn apply-each-wrapper
  "Given context  and Ring handler, apply specified wrapper to the handler."
  [context handler config-key wrapper]
  (if (fn? wrapper)
    (echo/with-inducer-name wrapper
      (wrapper handler context))
    (apply-wrapper-by-name "wrapper" config-key handler context wrapper)))


(defn apply-wrappers
  [context config-key wrappers]
  (util/induce context (fn [ctx each-wrapper]
                         (as-> (ctx-ring-handler ctx) <>
                           (apply-each-wrapper ctx <> config-key each-wrapper)
                           (assoc ctx (key ctx-ring-handler) <>)))
    wrappers))
