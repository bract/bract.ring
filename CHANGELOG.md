# bract.ring Change Log

## TODO

- [Todo] Distributed trace
- [Todo] Health check (cached for configured duration)
  - `{"status": "OK"}`
  - `{"status": "WARN", "disk": {"status": "WARN", "total": "5GB", "free": "10MB"}}`
  - `{"status": "ERROR", "mysql": {"status": "ERROR", "circuit-breaker": "TRIPPED"}}`
  - `{"status": "WARN", "mysql": {"status": "OK", "circuit-breaker": "CONNECTED"}, "cache": {"status": "ERROR"}}`
- [Todo] unexpected->500
- [Todo] Traffic drain
- [Todo] SSE streaming (requires ring-sse-middleware dependency)
- [Todo] Ring metrics  (requires ring-metrics dependency)
- [Todo] Log level override (requires logback dependency)


## [WIP] 0.5.0 / 2018-January-??

- Use bract.core 0.5.0
- Wrappers
  - [Todo] /info handler and middleware
  - [Todo] /ping handler and middleware
  - [Todo] uri-trailing-slash middleware
  - [Todo] uri-prefix-match middleware
  - [Todo] wrap-params-normalize middleware


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
