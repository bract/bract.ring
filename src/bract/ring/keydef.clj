;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.keydef
  (:require
    [keypin.core :as keypin]
    [keypin.util :as kputil]
    [bract.core.echo    :as echo]
    [bract.core.inducer :as inducer]))


(keypin/defkey  ; context keys
  ctx-ring-handler [:bract.ring/ring-handler fn? "Application Ring handler"])


(keypin/defkey
  cfg-health-codes ["bract.ring.health.codes" map? "Map of health status to HTTP status code"
                    {:parser kputil/any->edn
                     :default {:critical 503
                               :degraded 500
                               :healthy  200}}])
