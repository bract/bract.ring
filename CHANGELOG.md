# bract.ring Change Log

## TODO

- Key definitions
  - [Todo] Support for dual or multiple handlers starting at different web server ports
- Distributed trace
  - [Todo] Sync with https://w3c.github.io/distributed-tracing/report-trace-context.html
- [Idea] Define config for default (but how to distinguish when more then one server?)
  - host: `APP_HTTP_SERVER_HOST`
  - port: `APP_HTTP_SERVER_PORT`
  - thread-pool size etc.


## 0.6.2-0.2.0 / 2021-February-28

- Dependency update
  - Upgrade bract.core to `0.6.2`
- Breaking changes
  - Drop `bract.ring.util/nop`, `bract.ring.dev/nop` in favour of `bract.core.util/nop`
- New features
  - Add Ring middleware for traffic logging
  - Add Ring wrapper for traffic logging
  - Add support for [nginx-clojure-embedded](https://github.com/nginx-clojure/nginx-clojure/tree/master/nginx-clojure-embed)
    - `bract.ring.server/start-nginx-clojure-embedded-server`
  - Add DEV logger functions `bract.ring.dev/log-*` for `bract.ring.wrapper/traffic-log-wrapper`
    - Add DEV mode logger config in `config.dev.edn` file
- Improvements
  - Support HTTP `POST` method (with custom body) in `/ping` middleware
  - Print banner when HTTP server starts
  - Log events in wrappers
    - Health - config `"bract.ring.health.event.name"`
    - Info   - config `"bract.ring.info.event.name"`
    - Ping   - config `"bract.ring.ping.event.name"`
  - Echo message when server stopped
  - Include default config value for `bract.ring.server.options` in `bract/ring/config.edn`
- Documentation
  - Add _cljdoc_ badge
  - Reformat docstring for _cljdoc_
  - Add documentation page with context and config keys


## 0.6.1-0.1.0 / 2018-October-10

- Upgrade bract.core to 0.6.1


## 0.6.0-0.1.0 / 2018-May-16

- Upgrade bract.core to 0.6.0
- Key definitions
  - Context `:bract.ring/server-starter` starts server
  - Context `:bract.ring/server-stopper` schedules stopper function for a started server
  - Context `:bract.ring/server-options` options to be passed to server when starting
  - Config `"bract.ring.server.options"` options to start server with
- Inducer
  - Add `bract.ring.inducer/start-server` to easily start Ring server
- Add server-startup functions in `bract.ring.server` namespace (dependency not included)
  - Aleph    - `bract.ring.server/start-aleph-server`
  - HTTP-Kit - `bract.ring.server/start-http-kit-server`
  - Immutant - `bract.ring.server/start-immutant-server`
  - Jetty    - `bract.ring.server/start-jetty-server`
- Resources
  - Add `bract/ring/context.edn` with context entries
  - [BREAKING CHANGE] Rename `bract/ring/default.edn` to `bract/ring/config.edn`, removing non-config entries
  - [BREAKING CHANGE] Rename `bract/ring/devdelta.edn` to `bract/ring/config.dev.edn`
- Echo
  - Emit "server started" message to STDERR for all supported servers
  - Output server-started message to STDERR only when echo is disabled


## 0.5.1 / 2018-March-05

- Use bract.core 0.5.1


## 0.5.0 / 2018-February-18

- Use bract.core 0.5.0
- Add inducer `bract.ring.inducer/apply-middlewares` accepting Ring middleware
- Wrappers (can toggle each with config flag)
  - Health check (/health)
  - Info endpoint (/info)
  - Ping endpoint (/ping)
  - uri-trailing-slash
  - uri-prefix-match
  - params-normalize
  - unexpected->500
  - traffic-drain
  - distributed-trace
- Provide config files
  - `bract/ring/default.edn`
  - `bract/ring/devdelta.edn`


## 0.4.1 / 2017-August-08

- Use bract.core 0.4.1


## 0.4.0 / 2017-August-05

- Use the GA version of bract.core 0.4.0


## 0.4.0-alpha2 / 2017-August-01

- Use bract.core 0.4.0-alpha2


## 0.4.0-alpha1 / 2017-July-31

- Use bract.core 0.4.0-alpha1
- [BREAKING CHANGE] Rename `bract.ring.config` namespace to `bract.ring.keydef`


## 0.3.1 / 2017-June-30
- Use bract.core 0.3.1
- Update inducer docstring to reflect the context keys they use


## 0.3.0 / 2017-June-11
### Changed
- Use bract.core 0.3
- Add inducer `bract.ring.inducer/apply-wrappers` accepting Ring wrappers
- [BREAKING CHANGE] Remove unparameterized wrapper-applying inducers in favor of `apply-wrappers`
  - Remove inducer `bract.ring.inducer/ctx-apply-wrappers`
  - Remove inducer `bract.ring.inducer/cfg-apply-wrappers`
  - Remove config definition `bract.ring.config/ctx-wrappers`
  - Remove config definition `bract.ring.config/cfg-wrappers`


## 0.2.0 / 2017-June-04
### Changed
- Use `[bract.core "0.2.0"]`

### Added
- Config definition `bract.ring.config/ctx-wrappers`
- Config definition `bract.ring.config/cfg-wrappers`
- Inducer `bract.ring.inducer/ctx-apply-wrappers`
- Inducer `bract.ring.inducer/cfg-apply-wrappers`

### Removed
- Inducer `bract.ring.inducer/apply-wrappers-with-context`
- Inducer `bract.ring.inducer/apply-wrappers-with-config`
- Config definition `bract.ring.config/cfg-wrappers-context`
- Config definition `bract.ring.config/cfg-wrappers-config`


## 0.1.0 / 2017-April-25
### Added
- Development/test support for Ring handlers
- Bract inducer fn to apply configured wrappers (context)
- Bract inducer fn to apply configured wrappers (config)
