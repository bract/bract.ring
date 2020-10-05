# Introduction to bract.ring

This module provides [Ring](https://github.com/ring-clojure/ring) support for the Bract framework.


## Starting a web server

The inducer `bract.ring.inducer/start-server` can start a web server using a supplied
[Ring handler](https://github.com/ring-clojure/ring/wiki/Concepts#handlers). Include it
in your inducer chain and populate the following keys in the context:

```edn
  :bract.ring/ring-handler   (fn [request])  ; a valid Ring handler
  :bract.ring/server-starter bract.ring.server/start-http-kit-server
  :bract.ring/server-options {:port 3000}
```

You may choose any supported server starter from the `bract.ring.server` namespace. You may also put the server
options in the config (referred to by context key `:bract.core/config`) under the key `"bract.ring.server.options"`.


## Applying middleware to Ring handler

You may apply [middleware](https://github.com/ring-clojure/ring/wiki/Concepts#middleware) to a Ring handler
(required to be available under the context key `:bract.ring/ring-handler`) using the inducer
`bract.ring.inducer/apply-middlewares`:

```edn
  :bract.ring/ring-handler (fn [request])  ; a valid Ring handler
  :app/inducers            [(bract.ring.inducer/apply-middlewares
                              [ring.middleware.content-type/wrap-content-type
                               ring.middleware.params/wrap-params])]
```

The above example applies two middlewares to the Ring handler. In practice, you would add the Ring handler to the
context via an application inducer.


## Applying wrapper to Ring handler

A wrapper is like a middleware (see above section) function, but with arity `(fn [handler context])`. There are
several useful wrappers available in the `bract.ring.wrapper` namespace. Below is an example how you could apply
wrappers to a Ring handler:

```edn
  :bract.ring/ring-handler (fn [request])  ; a valid Ring handler
  :app/inducers            [(bract.ring.inducer/apply-wrappers
                              [bract.ring.wrapper/info-endpoint-wrapper
                               bract.ring.wrapper/ping-endpoint-wrapper])]
```

In practice, you would add the Ring handler to the context via an application inducer.


## Context keys

| Context key                | Value type                                | Description                        |
|----------------------------|-------------------------------------------|------------------------------------|
|`:bract.ring/ring-handler`  | arity-1 or arity-3 ring handler function  | Ring handler (sync or async)       |
|`:bract.ring/server-starter`|`(fn [handler options]) -> (fn stopper [])`| See section `Starting a web server`|
|`:bract.ring/server-stopper`|`(fn [context (fn stopper [])]) -> context`| Server stopper function            |
|`:bract.ring/server-options`| map                                       | Server options                     |


### Provided default context

The file `resources/bract/ring/context.edn` (which may be referred as `bract/ring/context.edn` from an application)
provides all wrappers from the namespace `bract.ring.wrapper` under the key `:bract.ring/wrappers`.


## Config keys

Legend:

    - FQFN: Fully qualified function name (string or symbol)
    - FNable: Function or FQFN
    - FNable-0: Zero arity function or FQFN
    - FNable-1: Arity-1 function or FQFN


| Config key                  | Value type     | Description                                           |
|-----------------------------|----------------|-------------------------------------------------------|
|`"bract.ring.server.options"`| map            | Server options                                        |


### Wrapper enabled check

The following config flags are looked up by respective Ring wrappers to determine whether to apply the wrapper:

| Config key                              | Value type | Description                            |
|-----------------------------------------|------------|----------------------------------------|
|`"bract.ring.health.check.enabled"`      | boolean    | Enable health check wrapper?           |
|`"bract.ring.info.endpoint.enabled"`     | boolean    | Enable /info endpoint wrapper?         |
|`"bract.ring.ping.endpoint.enabled"`     | boolean    | Enable /ping endpoint wrapper?         |
|`"bract.ring.uri.trailing.slash.enabled"`| boolean    | Enable URI trailing slash wrapper?     |
|`"bract.ring.uri.prefix.match.enabled"`  | boolean    | Enable URI prefix match wrapper?       |
|`"bract.ring.params.normalize.enabled"`  | boolean    | Enable Ring params normalize wrapper?  |
|`"bract.ring.unexpected.500.enabled"`    | boolean    | Enable unexpected response 500 wrapper?|
|`"bract.ring.traffic.drain.enabled"`     | boolean    | Enable traffic drain wrapper?          |
|`"bract.ring.distributed.trace.enabled"` | boolean    | Enable distributed trace wrapper?      |
|`"bract.ring.traffic.log.enabled"`       | boolean    | Enable traffic log wrapper?            |


### Wrapper config

The configs related to various wrappers are listed below:

#### Health check wrapper

| Config key                       | Value type       | Description                                                  |
|----------------------------------|------------------|--------------------------------------------------------------|
|`"bract.ring.health.check.uris"`  | vector of string | URIs for health check endpoint, e.g. `["/health" "/health/"]`|
|`"bract.ring.health.body.encoder"`| FNable-1         | (fn [data]) -> Ring response body, e.g. `clojure.core/pr-str`|
|`"bract.ring.health.content.type"`| string           | Ring response Content-type header, e.g. `"application/edn"`  |
|`"bract.ring.health.http.codes"`  |map keyword:string| HTTP codes, e.g. `{:critical 503 :degraded 500 :healthy 200}`|


#### /info endpoint wrapper

| Config key                       | Value type       | Description                                                  |
|----------------------------------|------------------|--------------------------------------------------------------|
|`"bract.ring.info.endpoint.uris"` | vector of string | URIs for /info endpoint, e.g. `["/info" "/info/"]`           |
|`"bract.ring.info.body.encoder"`  | FNable-1         | (fn [data]) -> Ring response body, e.g. `clojure.core/pr-str`|
|`"bract.ring.info.content.type"`  | string           | Ring response Content-type header, e.g. `"application/edn"`  |


#### /ping endpoint wrapper

| Config key                       | Value type       | Description                                                  |
|----------------------------------|------------------|--------------------------------------------------------------|
|`"bract.ring.ping.endpoint.uris"` | vector of string | URIs for /info endpoint, e.g. `["/info" "/info/"]`           |
|`"bract.ring.ping.endpoint.body"` |Ring response body| A valid Ring response body, e.g. `"pong"`                    |
|`"bract.ring.ping.content.type"`  | string           | Ring response Content-type header, e.g. `"text/plain"`       |


#### Trailing slash wrapper

| Config key                             | Value type | Description                                                  |
|----------------------------------------|------------|--------------------------------------------------------------|
|`"bract.ring.uri.trailing.slash.action"`| Keyword    | Operative keyword - `:add` or `:remove`                      |


#### Prefix match wrapper

| Config key                          | Value type | Description                                                  |
|-------------------------------------|------------|--------------------------------------------------------------|
|`"bract.ring.uri.prefix.match.token"`| string     | The prefix string to match                                   |
|`"bract.ring.uri.prefix.strip.flag"` | boolean    | Whether strip the prefix from the URI, e.g. `true`           |
|`"bract.ring.uri.prefix.backup.flag"`| boolean    | Whether backup the original URI in the request, e.g. `true`  |
|`"bract.ring.uri.prefix.backup.key"` | keyword    | Key to store original URI at in request, e.g. `:original-uri`|


#### Params normalize wrapper

| Config key                             | Value type | Description                                                  |
|----------------------------------------|------------|--------------------------------------------------------------|
|`"bract.ring.params.normalize.function"`|arity-1 FQFN| Function to normalize param value, e.g. `clojure.core/first` |


#### Unexpected->500 wrapper

| Config key                             | Value type | Description                                                  |
|----------------------------------------|------------|--------------------------------------------------------------|
|`"bract.ring.unexpected.response.fn"`   |arity-3 FQFN| Unexpected response handler e.g. `b.r.util/bad-response->500`|
|`"bract.ring.unexpected.exception.fn"`  |arity-2 FQFN| Exception handler, e.g. `b.r.util/exception->500`            |


#### Traffic drain wrapper

| Config key                             | Value type | Description                                                  |
|----------------------------------------|------------|--------------------------------------------------------------|
|`"bract.ring.traffic.conn.close.flag"`  | boolean    | Whether send `Connection: close` response header, e.g. `true`|


#### Distributed trace wrapper

| Config key                             | Value type | Description                                                  |
|----------------------------------------|------------|--------------------------------------------------------------|
|`"bract.ring.trace.trace.id.header"`    | string     | Trace ID header, e.g. `"x-trace-id"`                         |
|`"bract.ring.trace.parent.id.header"`   | string     | Parent ID header, e.g. `"x-trace-parent-id"`                 |
|`"bract.ring.trace.trace.id.req.flag"`  | boolean    | Whether Trace ID is required, e.g. `false`                   |
|`"bract.ring.trace.trace.id.valid.fn"`  |arity-1 FQFN| Function to return error on invalid Trace ID, `nil` otherwise|
|`"bract.ring.trace.trace.id.req.key"`   | keyword    | Request key to put trace ID under, e.g. `:trace-id`          |
|`"bract.ring.trace.span.id.req.key"`    | keyword    | Request key to put request ID under, e.g. `:span-id`         |
|`"bract.ring.trace.parent.id.req.key"`  | keyword    | Request key to put parent ID under, e.g. `:parent-id`        |


#### Traffic log wrapper

| Config key                             | Value type | Description                                                   |
|----------------------------------------|------------|---------------------------------------------------------------|
|`"bract.ring.traffic.log.options"`      | map        |Keys `:request-logger`, `:response-logger`, `:exception-logger`|


### Provided default config

The files `resources/bract/ring/config.edn` (which may be referred as `bract/ring/context.edn` from an application)

| File                                                   | Available in application as | Contents                 |
|--------------------------------------------------------|-----------------------------|--------------------------|
|[config.edn](../resources/bract/ring/config.edn)        |`bract/ring/config.edn`      | Useful defaults for prod |
|[config.dev.edn](../resources/bract/ring/config.dev.edn)|`bract/ring/config.dev.edn`  | Useful defaults DEV mode |
