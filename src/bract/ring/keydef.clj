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
  cfg-health-check-wrapper?       ["bract.ring.health.check.wrapper.enabled"       kputil/bool?
                                   "Is health-check wrapper enabled?"]
  cfg-info-wrapper?               ["bract.ring.info.wrapper.enabled"               kputil/bool?
                                   "Is info wrapper enabled"]
  cfg-ping-wrapper?               ["bract.ring.ping.wrapper.enabled"               kputil/bool?
                                   "Is ping wrapper enabled?"]
  cfg-uri-trailing-slash-wrapper? ["bract.ring.uri.trailing.slash.wrapper.enabled" kputil/bool?
                                   "Is URI trailing slash wrapper enabled?"]
  cfg-uri-prefix-match-wrapper?   ["bract.ring.uri.prefix.match.wrapper.enabled"   kputil/bool?
                                   "Is URI prefix match wrapper enabled?"]
  cfg-params-normalize-wrapper?   ["bract.ring.params.normalize.wrapper.enabled"   kputil/bool?
                                   "Is wrap-params normalize wrapper enabled?"]
  cfg-unexpected->500-wrapper?    ["bract.ring.unexpected.500.wrapper.enabled"     kputil/bool?
                                   "Is unexpected->500 wrapper enabled?"]
  cfg-traffic-drain-wrapper?      ["bract.ring.traffic.drain.wrapper.enabled"      kputil/bool?
                                   "Is traffic-drain wrapper enabled?"])


(keypin/defkey  ; config keys for wrappers
  cfg-health-codes ["bract.ring.health.codes" map? "Map of health status to HTTP status code"
                    {:parser kputil/any->edn
                     :default {:critical 503
                               :degraded 500
                               :healthy  200}}])
