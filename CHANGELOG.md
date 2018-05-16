# bract.ring Change Log

## TODO

- Key definitions
  - [Todo] Support for dual or multiple handlers starting at different web server ports
- Distributed trace
  - [Todo] Sync with https://w3c.github.io/distributed-tracing/report-trace-context.html


## [WIP] 0.6.0-0.1.0 / 2018-May-??

- [Todo] Upgrade bract.core to 0.6.0


## 0.6.0-beta3 / 2018-May-12

- Upgrade bract.core to version `0.6.0-beta3`


## 0.6.0-beta2 / 2018-May-01

- Upgrade bract.core to version `0.6.0-beta2`


## 0.6.0-beta1 / 2018-March-27

- Upgrade bract.core to 0.6.0-beta1
- Output server-started message to STDERR only when echo is disabled


## 0.6.0-alpha4 / 2018-March-25

- Upgrade bract.core to 0.6.0-alpha4
- Emit "server started" message to STDERR for all supported servers


## 0.6.0-alpha3 / 2018-March-23

- Use bract.core 0.6.0-alpha3


## 0.6.0-alpha2 / 2018-March-22

- Use bract.core 0.6.0-alpha2


## 0.6.0-alpha1 / 2018-March-14

- Use bract.core 0.6.0-alpha1
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
