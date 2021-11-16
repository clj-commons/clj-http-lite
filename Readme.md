# `clj-http-lite` [![cljdoc badge](https://cljdoc.xyz/badge/org.martinklepsch/clj-http-lite)](https://cljdoc.xyz/d/org.martinklepsch/clj-http-lite/CURRENT) [![CI](https://github.com/martinklepsch/clj-http-lite/workflows/Tests/badge.svg)](https://github.com/martinklepsch/clj-http-lite/actions)

A Clojure HTTP library similar to [clj-http](http://github.com/dakrone/clj-http), but more lightweight (and dependency-free by default). Compatible with GraalVM.

> This is a somewhat maintained fork of the original [`hiredman/clj-http-lite`](https://github.com/hiredman/clj-http-lite) repo.

[Installation](#installation) | [Usage](#usage) | [Known Issues](#known-issues) | [Design](#design) | [Development](#development)

## Installation

`clj-http-lite` is available as a Maven artifact from [Clojars](https://clojars.org/org.martinklepsch/clj-http-lite):

```clojure
[org.martinklepsch/clj-http-lite "0.4.3"]
```

## Differences from clj-http

- Instead of Apache HTTP client, clj-http-lite uses HttpURLConnection
- No automatic JSON decoding for response bodies
- No cookie support
- No proxy-ing DELETEs with body
- No multipart form uploads
- No persistent connection support
- namespace rename clj-http.* -> clj-http.lite.*

## Usage

The main HTTP client functionality is provided by the
`clj-http-lite.client` namespace:

```clojure
(require '[clj-http-lite.client :as client])
```

The client supports simple `get`, `head`, `put`, `post`, and `delete`
requests. Responses are returned as Ring-style response maps:

```clojure
(client/get "http://google.com")
=> {:status 200
    :headers {"date" "Sun, 01 Aug 2010 07:03:49 GMT"
              "cache-control" "private, max-age=0"
              "content-type" "text/html; charset=ISO-8859-1"
              ...}
    :body "<!doctype html>..."}
```

More example requests:

```clojure
(client/get "http://site.com/resources/id")

(client/get "http://site.com/resources/3" {:accept :json})

;; Various options:
(client/post "http://site.com/api"
  {:basic-auth ["user" "pass"]
   :body "{\"json\": \"input\"}"
   :headers {"X-Api-Version" "2"}
   :content-type :json
   :socket-timeout 1000
   :conn-timeout 1000
   :accept :json})

;; Need to contact a server with an untrusted SSL cert?
(client/get "https://alioth.debian.org" {:insecure? true})

;; If you don't want to follow-redirects automatically:
(client/get "http://site.come/redirects-somewhere" {:follow-redirects false})

;; Send form params as a urlencoded body
(client/post "http//site.com" {:form-params {:foo "bar"}})

;; Basic authentication
(client/get "http://site.com/protected" {:basic-auth ["user" "pass"]})
(client/get "http://site.com/protected" {:basic-auth "user:pass"})

;; Query parameters
(client/get "http://site.com/search" {:query-params {"q" "foo, bar"}})
```

The client will also follow redirects on the appropriate `30*` status
codes.

The client transparently accepts and decompresses the `gzip` and
`deflate` content encodings.

### Input coercion

```clojure
;; body as a byte-array
(client/post "http://site.com/resources" {:body my-byte-array})

;; body as a string
(client/post "http://site.com/resources" {:body "string"})

;; :body-encoding is optional and defaults to "UTF-8"
(client/post "http://site.com/resources"
             {:body "string" :body-encoding "UTF-8"})

;; body as a file
(client/post "http://site.com/resources"
             {:body (clojure.java.io/file "/tmp/foo") :body-encoding
             "UTF-8"})

;; :length is NOT optional for passing an InputStream in
(client/post "http://site.com/resources"
             {:body (clojure.java.io/input-stream "/tmp/foo")
              :length 1000})
```

### Output coercion

```clojure
;; The default output is a string body
(client/get "http://site.com/foo.txt")

;; Coerce as a byte-array
(client/get "http://site.com/favicon.ico" {:as :byte-array})

;; Coerce as something other than UTF-8 string
(client/get "http://site.com/string.txt" {:as "UTF-16"})

;; Try to automatically coerce the output based on the content-type
;; header (this is currently a BETA feature!)
(client/get "http://site.com/foo.bar" {:as :auto})

;; Return the body as a stream
(client/get "http://site.com/bigrequest.html" {:as :stream})
;; Note that the connection to the server will NOT be closed until the
;; stream has been read
```

A more general `request` function is also available, which is useful
as a primitive for building higher-level interfaces:

```clojure
(defn api-action [method path & [opts]]
  (client/request
    (merge {:method method :url (str "http://site.com/" path)} opts)))
```

### Exceptions

> NOTE: The newer `clj-http-lite.client` ns does not use Slingshot.
> The legacy `clj-http.lite.client` remains available, as long as your project explicitly adds a Slingshot dependency.

The client will throw exceptions on, well, exceptional status
codes. clj-http will `throw` a vanilla `ex-info`.

```clojure
(client/get "http://site.com/broken")
#error {
 :cause "clj-http: status 404"
 :data {:response {:headers {"content-type" "text/html; charset=UTF-8", "content-length" "1569"},
                   :status 404,
                   :body "..."}}}

;; You can also ignore exceptions and handle them yourself:
(client/get "http://site.com/broken" {:throw-exceptions false})
;; Or ignore an unknown host (methods return 'nil' if this is set to
;; true and the host does not exist:
(client/get "http://aoeuntahuf89o.com" {:ignore-unknown-host? true})
````
(spacing added by me to be human readable)

### Proxies

A proxy can be specified by setting the Java properties:
`<scheme>.proxyHost` and `<scheme>.proxyPort` where `<scheme>` is the client
scheme used (normally 'http' or 'https').

## Faking clj-http responses

If you need to fake clj-http responses (for things like testing and
such), check out the
[clj-http-fake](https://github.com/myfreeweb/clj-http-fake) library.

## Known Issues

- Nested form params [aren't serialized correctly](https://github.com/hiredman/clj-http-lite/issues/15). There's an easy workaround however:

    ```clojure
    :form-params {"toplevel" {"nested" some-data}} ; doesn't work
    :form-params {"toplevel[nested]" some-data}    ; works
    ```

- If you issue HTTPS connections, [Native Image](https://www.graalvm.org/docs/reference-manual/native-image/) compilation requires an additional parameter in order to enable its support in the generated image.

  If you get the following kind of error:

      Exception in thread "main" java.net.MalformedURLException: Accessing an URL protocol that was not enabled.  
      The URL protocol https is supported but not enabled by default. It must be enabled by adding the
      -H:EnableURLProtocols=https option to the native-image command.

  Then add either `-H:EnableURLProtocols=https` or `--enable-https` option to your compilation step.

## Design

The design of `clj-http` is inspired by the
[Ring](http://github.com/mmcgrana/ring) protocol for Clojure HTTP
server applications.

The client in `clj-http.lite.core` makes HTTP requests according to a given
Ring request map and returns Ring response maps corresponding to the
resulting HTTP response. The function `clj-http-lite.client/request` uses
Ring-style middleware to layer functionality over the core HTTP
request/response implementation. Methods like `clj-http-lite.client/get`
are sugar over this `clj-http-lite.client/request` function.

## Development

To run the tests:

    $ lein deps
    $ lein test

    Run all tests (including integration):
    $ lein test :all

    Run tests against 1.2.1, 1.3, 1.4 and 1.5
    $ lein all do clean, test :all

## License

Released under the MIT License:
<http://www.opensource.org/licenses/mit-license.php>
