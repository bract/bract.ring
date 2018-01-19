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


(keypin/defkey  ; config keys for wrappers enabled flag
  {:parser kputil/any->edn
   :default true}
  cfg-health-check-wrapper?       ["bract.ring.health.check.enabled"       kputil/bool? "Health-check enabled?"]
  cfg-info-wrapper?               ["bract.ring.info.enabled"               kputil/bool? "Info endpoint enabled?"]
  cfg-ping-wrapper?               ["bract.ring.ping.enabled"               kputil/bool? "Ping endpoint enabled?"]
  cfg-uri-trailing-slash-wrapper? ["bract.ring.uri.trailing.slash.enabled" kputil/bool? "URI trailing slash enabled?"]
  cfg-uri-prefix-match-wrapper?   ["bract.ring.uri.prefix.match.enabled"   kputil/bool? "Is URI prefix match enabled?"]
  cfg-params-normalize-wrapper?   ["bract.ring.params.normalize.enabled"   kputil/bool? "Is params normalize enabled?"]
  cfg-unexpected->500-wrapper?    ["bract.ring.unexpected.500.enabled"     kputil/bool? "Is unexpected->500 enabled?"]
  cfg-traffic-drain-wrapper?      ["bract.ring.traffic.drain.enabled"      kputil/bool? "Is traffic-drain enabled?"])


(keypin/defkey  ; config keys for wrappers
  cfg-health-codes ["bract.ring.health.codes" map? "Map of health status to HTTP status code"
                    {:parser kputil/any->edn
                     :default {:critical 503
                               :degraded 500
                               :healthy  200}}])
