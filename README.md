# `clj-http-lite` [![cljdoc badge](https://cljdoc.org/badge/org.clj-commons/clj-http-lite)](https://cljdoc.org/d/org.clj-commons/clj-http-lite) [![CI tests](https://github.com/clj-commons/clj-http-lite/workflows/Tests/badge.svg)](https://github.com/clj-commons/clj-http-lite/actions) [![Clojars](https://img.shields.io/clojars/v/org.clj-commons/clj-http-lite.svg)](https://clojars.org/org.clj-commons/clj-http-lite) [![bb compatible](https://raw.githubusercontent.com/babashka/babashka/master/logo/badge.svg)](https://babashka.org)

A Clojure HTTP library similar to [clj-http](http://github.com/dakrone/clj-http), but more lightweight. Compatible with Babashka and GraalVM.

> This is a clj-commons maintained fork of the archived [`hiredman/clj-http-lite`](https://github.com/hiredman/clj-http-lite) repo.

[Installation](#installation) | [Usage](#usage) | [Design](#design) | [Development](#development)

## Installation

`clj-http-lite` is available as a Maven artifact from [Clojars](https://clojars.org/org.clj-commons/clj-http-lite):

```clojure
[org.clj-commons/clj-http-lite "0.4.392"]
```

## Differences from clj-http

- Instead of Apache HttpClient, clj-http-lite uses HttpURLConnection
- No automatic JSON decoding for response bodies
- No automatic request body encoding beyond charset and url encoding of form params
- No cookie support
- No multipart form uploads
- No persistent connection support
- Fewer options
- namespace rename clj-http.* -> clj-http.lite.*

Like its namesake, clj-http-lite is light and simple, but ping us if there is some clj-http feature you'd like to see in clj-http-lite. We can discuss.
## History

- Sep 2011 - [dakrone/clj-http](https://github.com/dakrone/clj-http) created (and is still actively maintained)
- Feb 2012 - [hiredman/clj-http-lite](https://github.com/hiredman/clj-http-lite) (now archived) forked from `dakrone/clj-http` to use Java's HttpURLConnection instead of Apache HttpClient.
- Jul 2018 - `martinklepsch/clj-http-lite` forked from `hiredman/clj-http-lite` for new development and maintenance
- Nov 2021 - Martin transfered his fork to `clj-commons/clj-http-lite` so it could get the ongoing love it needs from the Clojure community

## Usage

The main HTTP client functionality is provided by the
`clj-http.lite.client` namespace:

```clojure
(require '[clj-http.lite.client :as client])
```

The client supports simple `get`, `head`, `put`, `post`, and `delete`
requests. They all return Ring-style response maps:

```clojure
(client/get "https://google.com")
=> {:status 200
    :headers {"date" "Wed, 17 Aug 2022 21:37:58 GMT"
              "cache-control" "private, max-age=0"
              "content-type" "text/html; charset=ISO-8859-1"
              ...}
    :body "<!doctype html>..."}
```

**TIP**: We encourage you to try out these examples in your REPL, `httpbin.org` is a free HTTP test playground and used in many examples.

```clojure
(client/get "https://httpbin.org/user-agent")

;; Tell the server you'd like a json response
(client/get "https://httpbin.org/user-agent" {:accept :json})

;; Or maybe you'd like html back
(client/get "https://httpbin.org/html" {:accept "text/html"})

;; Various options
(client/post "https://httpbin.org/anything"
  {:basic-auth ["joe" "cool"]
   :body "{\"json\": \"input\"}"
   :headers {"X-Api-Version" "2"}
   :content-type :json
   :socket-timeout 1000
   :conn-timeout 1000
   :accept :json})

;; Need to contact a server with an untrusted SSL cert?
(client/get "https://expired.badssl.com" {:insecure? true})

;; By default we automatically follow 30* redirects...
(client/get "https://httpbin.org/redirect-to?url=https%3A%2F%2Fclojure.org")

;; ... but you don't have to
(client/get "https://httpbin.org/redirect-to?url=https%3A%2F%2Fclojure.org"
            {:follow-redirects false})

;; Send form params as a urlencoded body
(client/post "https://httpbin.org/post" {:form-params {:foo "bar"}})

;; Basic authentication
(client/get "https://joe:cool@httpbin.org/basic-auth/joe/cool")
(client/get "https://httpbin.org/basic-auth/joe/cool" {:basic-auth ["joe" "cool"]})
(client/get "https://httpbin.org/basic-auth/joe/cool" {:basic-auth "joe:cool"})

;; Query parameters can be specified as a map
(client/get "https://httpbin.org/get" {:query-params {"q" "foo, bar"}})
```

The client transparently accepts and decompresses the `gzip` and `deflate` content encodings.

```Clojure
(client/get "https://httpbin.org/gzip")

(client/get "https://httpbin.org/deflate")
```

### Nested params

Nested parameter `{:a {:b 1}}` in `:form-params` or `:query-params` is automatically flattened to `a[b]=1`.

```clojure
(-> (client/get "https://httpbin.org/get"
                {:query-params {:one {:two 2 :three 3}}})
    :body
    println)
{
  "args": {
    "one[three]": "3",
    "one[two]": "2"
  },
  ...
}

(-> (client/post "https://httpbin.org/post"
                 {:form-params {:one {:two 2
                                      :three {:four {:five 5}}}
                                :six 6}})
    :body
    println)
{
  ...
  "form": {
    "one[three][four][five]": "5",
    "one[two]": "2",
    "six": "6"
  },
  ...
}
```

### Request body coercion

```clojure
;; body as byte-array
(client/post "https://httbin.org/post" {:body (.getBytes "testing123")})

;; body from a string
(client/post "https://httpbin.org/post" {:body "testing456"})

;; string :body-encoding is optional and defaults to "UTF-8"
(client/post "https://httpbin.org/post"
             {:body "mystring" :body-encoding "UTF-8"})

;; body from a file
(require '[clojure.java.io :as io])
(spit "clj-http-lite-test.txt" "from a file")
(client/post "https://httpbin.org/post"
             {:body (io/file "clj-http-lite-test.txt")
              :body-encoding "UTF-8"})

;; from a stream
(with-open [is (io/input-stream "clj-http-lite-test.txt")]
  (client/post "https://httpbin.org/post"
               {:body (io/input-stream "clj-http-lite-test.txt")})  )
```

### Output body coercion

```clojure
;; The default response body is a string body
(client/get "https://clojure.org")

;; Coerce to a byte-array
(client/get "http://clojure.org" {:as :byte-array})

;; Coerce to a string with using a specific charset, default is UTF-8
(client/get "http://clojure.org" {:as "US-ASCII"})

;; Try to automatically coerce the body based on the content-type
;; response header charset
(client/get "https://google.com" {:as :auto})

;; Return the body as a stream
;; Note that the connection to the server will NOT be closed until the
;; stream has been read
(let [res (client/get "https://clojure.org" {:as :stream})]
  (with-open [body-stream (:body res)]
    (slurp body-stream)))
```

A more general `request` function is also available, which is useful
as a primitive for building higher-level interfaces:

```clojure
(defn api-action [method path & [opts]]
  (client/request
    (merge {:method method :url (str "https://some.api/" path)} opts)))
```

### Exceptions

The client will throw exceptions on, exceptional HTTP status
codes. Clj-http-lite throws an `ex-info` with the response as `ex-data`.

```clojure
(client/get "https://httpbin.org/404")
;; => ExceptionInfo clj-http: status 404  clojure.core/ex-info (core.clj:4617)

(-> *e ex-data :status)
;; => 404

(-> *e ex-data keys)
;; => (:headers :status :body)
```

You can suppress HTTP status exceptions and handle them yourself:

``` clojure
(client/get "https://httpbin.org/404" {:throw-exceptions false})
```

Or ignore an unknown host (methods return 'nil' if this is set to true and the host does not exist:

``` clojure
(client/get "http://aoeuntahuf89o.com" {:ignore-unknown-host? true})
;; => nil
```

### Proxies

A proxy can be specified by setting the Java properties:
`<scheme>.proxyHost` and `<scheme>.proxyPort` where `<scheme>` is the client
scheme used (normally 'http' or 'https').

## Mocking clj-http responses

Mocking responses from the clj-http-lite client in tests is easily accomplished with e.g. `with-redefs`:

```clojure
(defn my-http-function []
  (let [response (client/get "https://example.org")]
    (when (= 200 (:status response))
      (:body response))))

(deftest my-http-function-test
  (with-redefs [client/get (fn [_] {:status 200 :headers {"content-type" "text/plain"} :body "OK"})]
    (is (= (my-http-function) "OK"))))
```

More advanced mocking may be performed by matching attributes in the `request`, like the `mock-response` function below.

```clojure
(ns http-test
  (:require [clojure.data.json :as json]
            [clojure.test :refer [deftest is testing]]
            [clj-http.lite.client :as client]))

(defn send-report [data]
  (:body (client/post "https://example.com/reports" {:body data})))

(defn get-users []
  (json/read-str (:body (client/get "https://example.com/users"))))

(defn get-admin []
  (let [response (client/get "https://example.com/admin")]
    (if (= 200 (:status response))
      (:body response)
      "403 Forbidden")))

(defn mock-response [{:keys [url method body] :as request}]
  (condp = [url method]
    ["https://example.com/reports" :post]
    {:status  201 :headers {"content-type" "text/plain"} :body (str "created: " body)}

    ["https://example.com/users" :get]
    {:status 200 :headers {"content-type" "application/json"} :body (json/write-str ["joe" "jane" "bob"])}

    ["https://example.com/admin" :get]
    {:status 403 :headers {"content-type" "text/plain"} :body "forbidden"}

    (throw (ex-info "unexpected request" request))))

(deftest send-report-test
  (with-redefs [client/request mock-response]
    (testing "sending report"
      (is (= (send-report {:balance 100}) "created: {:balance 100}")))
    (testing "list users"
      (is (= (get-users) ["joe" "jane" "bob"])))
    (testing "access admin page"
      (is (= (get-admin) "403 Forbidden")))))
```

## GraalVM Native Image Tips

You'll need to enable url protocols when building your native image.

See [GraalVM docs](https://www.graalvm.org/22.2/reference-manual/native-image/dynamic-features/URLProtocols/).

## Design

The design of `clj-http` (and therefore `clj-http-lite`) is inspired by the
[Ring](https://github.com/ring-clojure/ring) protocol for Clojure HTTP
server applications.

The client in `clj-http.lite.core` makes HTTP requests according to a given
Ring request map and returns Ring response maps corresponding to the
resulting HTTP response. The function `clj-http.lite.client/request` uses
Ring-style middleware to layer functionality over the core HTTP
request/response implementation. Methods like `clj-http.lite.client/get`
are sugar over this `clj-http.lite.client/request` function.

## Development

### Clojure JVM Tests

To run tests for the JVM:

```shell
$ bb clean
$ bb deps

Run all Clojure tests against minimum supported version of Clojure (1.8)
$ clojure -M:test

Run Clojure against a specific Clojure version, for example 1.11
$ clojure -M:1.11:test
```

### Babashka Tests

To run a small suite of sanity tests for babashka (found under ./bb]):

```shell
$ bb test:bb
```

### Linting

Our CI workflow lints sources with clj-kondo, and you can too!

```shell
$ bb lint
```

### Release

To release a new version, run `bb publish` which will push a new tag. CI will
take care of the rest.

## License

Released under the MIT License:
<http://www.opensource.org/licenses/mit-license.php>
