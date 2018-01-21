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
  {:pred kputil/bool?
   :parser kputil/any->edn
   :default true}
  cfg-health-check-wrapper?       ["bract.ring.health.check.enabled"       {:desc "Health-check enabled?"}]
  cfg-info-endpoint-wrapper?      ["bract.ring.info.endpoint.enabled"      {:desc "Info endpoint enabled?"}]
  cfg-ping-endpoint-wrapper?      ["bract.ring.ping.endpoint.enabled"      {:desc "Ping endpoint enabled?"}]
  cfg-uri-trailing-slash-wrapper? ["bract.ring.uri.trailing.slash.enabled" {:desc "URI trailing slash enabled?"}]
  cfg-uri-prefix-match-wrapper?   ["bract.ring.uri.prefix.match.enabled"   {:desc "URI prefix match enabled?"}]
  cfg-params-normalize-wrapper?   ["bract.ring.params.normalize.enabled"   {:desc "Params normalize enabled?"}]
  cfg-unexpected->500-wrapper?    ["bract.ring.unexpected.500.enabled"     {:desc "Unexpected->500 enabled?"}]
  cfg-traffic-drain-wrapper?      ["bract.ring.traffic.drain.enabled"      {:desc "Traffic-drain enabled?"}])


(keypin/defkey  ; config keys for wrappers
  cfg-health-check-uris           ["bract.ring.health.check.uris"       vector? "Vector of health check endpoint URIs"
                                   {:parser kputil/any->edn
                                    :default ["/health" "/health/"]}]
  cfg-health-check-body-encoder   ["bract.ring.health.body.encoder"     fn?     "Function to encode health-check data"
                                   {:parser kputil/str->var->deref
                                    :default pr-str}]
  cfg-health-check-content-type   ["bract.ring.health.content.type"     string? "Content type for health-check body"
                                   {:default "application/edn"}]
  cfg-health-check-http-codes     ["bract.ring.health.check.http.codes" map?    "Map of health status to HTTP status"
                                   {:parser kputil/any->edn
                                    :default {:critical 503
                                              :degraded 500
                                              :healthy  200}}])
