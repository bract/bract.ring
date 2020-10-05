;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns bract.ring.keydef
  (:require
    [clojure.string :as string]
    [keypin.core :as keypin]
    [keypin.util :as kputil]
    [bract.core.echo    :as echo]
    [bract.core.inducer :as inducer]
    [bract.core.keydef  :as core-kdef]
    [bract.core.util    :as core-util]
    [bract.ring.util    :as ring-util]))


(keypin/defkey  ; context keys
  ctx-ring-handler   [:bract.ring/ring-handler    fn?  "Application Ring handler"]
  ctx-server-starter [:bract.ring/server-starter  fn?  "Ring server starter (fn [handler options]) -> (fn stopper [])"
                      {:parser kputil/str->fn}]
  ctx-server-stopper [:bract.ring/server-stopper  fn?  "Ring server stopper (fn [context (fn stopper [])]) -> context"
                      {:parser kputil/str->fn
                       :default (fn [context stopper] (assoc context (key core-kdef/ctx-stopper) stopper))}]
  ctx-server-options [:bract.ring/server-options  map? "Ring server options" {:parser kputil/any->edn}])


(keypin/defkey  ; config keys for Ring server
  cfg-server-options ["bract.ring.server.options" map? "Ring web server options" {:parser kputil/any->edn
                                                                                  :default {}}])


(keypin/defkey  ; config keys for wrappers enabled flag
  {:pred kputil/bool?
   :parser kputil/any->edn}
  cfg-health-check-wrapper?       ["bract.ring.health.check.enabled"       {:desc "Health-check enabled?"}]
  cfg-info-endpoint-wrapper?      ["bract.ring.info.endpoint.enabled"      {:desc "Info endpoint enabled?"}]
  cfg-ping-endpoint-wrapper?      ["bract.ring.ping.endpoint.enabled"      {:desc "Ping endpoint enabled?"}]
  cfg-uri-trailing-slash-wrapper? ["bract.ring.uri.trailing.slash.enabled" {:desc "URI trailing slash enabled?"}]
  cfg-uri-prefix-match-wrapper?   ["bract.ring.uri.prefix.match.enabled"   {:desc "URI prefix match enabled?"}]
  cfg-params-normalize-wrapper?   ["bract.ring.params.normalize.enabled"   {:desc "Params normalize enabled?"}]
  cfg-unexpected->500-wrapper?    ["bract.ring.unexpected.500.enabled"     {:desc "Unexpected->500 enabled?"}]
  cfg-traffic-drain-wrapper?      ["bract.ring.traffic.drain.enabled"      {:desc "Traffic-drain enabled?"}]
  cfg-distributed-trace-wrapper?  ["bract.ring.distributed.trace.enabled"  {:desc "Distributed trace enabled?"}]
  cfg-traffic-log-wrapper?        ["bract.ring.traffic.log.enabled"        {:desc "Traffic log enabled?"}])


(keypin/defkey  ; config keys for health check wrapper
  cfg-health-check-uris           ["bract.ring.health.check.uris"       vector? "Vector of health check endpoint URIs"
                                   {:parser kputil/any->edn}]
  cfg-health-body-encoder         ["bract.ring.health.body.encoder"     fn?     "Function to encode health-check data"
                                   {:parser kputil/any->var->deref}]
  cfg-health-content-type         ["bract.ring.health.content.type"     string? "Content type for health-check body"]
  cfg-health-http-codes           ["bract.ring.health.http.codes"       map?    "Map of health status to HTTP status"
                                   {:parser kputil/any->edn}])


(keypin/defkey  ; config keys for info endpoint wrapper
  cfg-info-endpoint-uris          ["bract.ring.info.endpoint.uris"      vector? "Vector of info endpoint URIs"
                                   {:parser kputil/any->edn}]
  cfg-info-body-encoder           ["bract.ring.info.body.encoder"       fn?     "Function to encode info data as body"
                                   {:parser kputil/any->var->deref}]
  cfg-info-content-type           ["bract.ring.info.content.type"       string? "Content type for info body"])


(keypin/defkey  ; config keys for ping endpoint wrapper
  cfg-ping-endpoint-uris          ["bract.ring.ping.endpoint.uris"      vector? "Vector of ping endpoint URIs"
                                   {:parser kputil/any->edn}]
  cfg-ping-endpoint-body          ["bract.ring.ping.endpoint.body"      string? "String body for ping response"]
  cfg-ping-content-type           ["bract.ring.ping.content.type"       string? "Content type for ping body"])


(keypin/defkey  ; config keys for URI trailing slash wrapper
  cfg-uri-trailing-slash-action   ["bract.ring.uri.trailing.slash.action" {:pred (every-pred keyword? #{:add :remove})
                                                                           :desc "Action keyword :add or :remove"
                                                                           :parser kputil/any->edn}])


(keypin/defkey  ; config keys for URI trailing slash wrapper
  cfg-uri-prefix-match-token      ["bract.ring.uri.prefix.match.token"  string? "URI prefix to be matched"]
  cfg-uri-prefix-strip-prefix?    ["bract.ring.uri.prefix.strip.flag"   kputil/bool? "Strip prefix from the URI?"
                                   {:parser  kputil/any->edn}]
  cfg-uri-prefix-backup-uri?      ["bract.ring.uri.prefix.backup.flag"  kputil/bool? "Backup original URI?"
                                   {:parser  kputil/any->edn}]
  cfg-uri-prefix-backup-key       ["bract.ring.uri.prefix.backup.key"   some?   "Backup key in request map to save URI"
                                   {:parser  kputil/any->edn}])


(keypin/defkey  ; config keys for URI trailing slash wrapper
  cfg-params-normalize-function   ["bract.ring.params.normalize.function" fn?   "Function to normalize request params"
                                   {:parser kputil/any->var->deref}])


(keypin/defkey  ; config keys for unexpected->500 wrapper
  cfg-unexpected-response-fn      ["bract.ring.unexpected.response.fn"  fn? "Fn (fn [req res cause]) for bad responses"
                                   {:parser kputil/any->var->deref}]
  cfg-unexpected-exception-fn     ["bract.ring.unexpected.exception.fn" fn? "Fn (fn [req ex]) to handle exception"
                                   {:parser kputil/any->var->deref}])


(keypin/defkey  ; config keys for traffic drain wrapper
  cfg-traffic-drain-conn-close?   ["bract.ring.traffic.conn.close.flag" kputil/bool? "Send 'conn close' header?"
                                   {:parser kputil/any->bool}])


(keypin/defkey  ; config keys for distributed trace wrapper
  cfg-trace-trace-id-header       ["bract.ring.trace.trace.id.header"   string? "HTTP header for trace ID"]
  cfg-trace-parent-id-header      ["bract.ring.trace.parent.id.header"  string? "HTTP header for parent ID"]
  cfg-trace-trace-id-required?    ["bract.ring.trace.trace.id.req.flag" kputil/bool? "Is trace ID required?"
                                   {:parser kputil/any->bool}]
  cfg-trace-trace-id-validator    ["bract.ring.trace.trace.id.valid.fn" fn?     "Validator (fn [trace-id]) -> error?"
                                   {:parser kputil/any->var->deref}]
  cfg-trace-trace-id-request-key  ["bract.ring.trace.trace.id.req.key"  some?   "Request key to store trace ID at"
                                   {:parser  kputil/any->edn}]
  cfg-trace-span-id-request-key   ["bract.ring.trace.span.id.req.key"   some?   "Request key to store span ID at"
                                   {:parser  kputil/any->edn}]
  cfg-trace-parent-id-request-key ["bract.ring.trace.parent.id.req.key" some?   "Request key to store parent ID at"
                                   {:parser  kputil/any->edn}])
