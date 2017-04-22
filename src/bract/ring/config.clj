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
    [keypin.util :as kputil]))


(keypin/defkey
  ;; context keys
  ctx-ring-handler [:bract.ring/ring-handler fn?     "Application Ring handler"]
  ;; config keys
  cfg-wrappers     ["bract.ring.wrappers"    vector? "Fully qualified wrapper fn names"
                    {:parser kputil/any->edn}])


(defn update-handler
  "Given a context and a Ring middleware `(fn [handler & args]) -> handler`, apply the middleware to the Ring handler
  in the context and any supplied arguments, and return context with the updated Ring handler."
  [context f & args]
  (let [handler (ctx-ring-handler context)]
    (apply update context (key ctx-ring-handler) f args)))
