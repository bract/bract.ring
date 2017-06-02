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
    [bract.ring.config :as config]))


(defn ctx-apply-wrappers
  "Given a context with Ring handler and Ring handler wrappers (under the context key :bract.ring/wrappers), i.e. a
  seq of `(fn [handler context]) -> handler`, apply them in order finally updating the context with the handler."
  [context]
  (config/apply-wrappers
    context
    (key config/cfg-wrappers)
    (config/ctx-wrappers context)))


(defn cfg-apply-wrappers
  "Given a context with Ring handler and Ring handler wrappers (under the config key \"bract.ring.wrappers\"), i.e. a
  seq of `(fn [handler context]) -> handler`, apply them in order finally updating the context with the handler."
  [context]
  (config/apply-wrappers
    context
    (key config/cfg-wrappers)
    (-> context bc-config/ctx-config config/cfg-wrappers)))
