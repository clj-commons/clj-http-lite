(ns clj-http.lite.client-sanity-test
  "A small subset of tests suitable for sanity testing."
  (:require [clj-http.lite.client :as client]
            [cheshire.core :as json]
            [matcher-combinators.test]
            [matcher-combinators.matchers :as m]
            [clojure.test :as t :refer [deftest is]]))

(deftest client-test
  (is (= 200 (:status (client/get "https://www.clojure.org" {:throw-exceptions false}))))

  (is (match? {:status  200
               :body (m/via json/decode {"method" "GET"
                                         "args" {"foo1" ["bar1"]
                                                 "foo2" ["bar2"]}})}
              (client/get "https://httpbingo.org/get?foo1=bar1&foo2=bar2" {:throw-exceptions false})))

  (is (match? {:status 200
               :body (m/via json/decode {"method" "POST"
                                         "headers" {"Content-Type" m/absent}
                                         "args" {}
                                         "data" ""})}
              (client/post "https://httpbingo.org/post" {:throw-exceptions false})))

  (is (match? {:status 200
               :body (m/via json/decode {"method" "POST"
                                         "headers" {"Content-Type" ["application/json; charset=UTF-8"]
                                                    "X-Hasura-Role" ["admin"]}
                                         "data" "{\"a\": 1}"})}
              (client/post "https://httpbingo.org/post"
                           {:body "{\"a\": 1}"
                            :headers {"X-Hasura-Role" "admin"}
                            :content-type :json
                            :accept :json
                            :throw-exceptions false})))

  (is (match? {:status 200
               :body (m/via json/decode {"method" "PUT"
                                         "headers" {"Content-Type" ["application/json; charset=UTF-8"]
                                                    "X-Hasura-Role" ["admin"]}
                                         "data" "{\"a\": 1}"})}
              (client/put "https://httpbingo.org/put"
                          {:body "{\"a\": 1}"
                           :headers {"X-Hasura-Role" "admin"}
                           :content-type :json
                           :accept :json
                           :throw-exceptions false}))))

(deftest exception-test
  (try (client/get "https://httpbingo.org/status/404")
       (is false "should not reach here")
       (catch Exception e
         (is (:headers (ex-data e))))))

(deftest insecure-test
  (is (thrown? Exception
               (client/get "https://expired.badssl.com")))
  (is (= 200 (:status (client/get "https://expired.badssl.com" {:insecure? true}))))
  (is (thrown? Exception
               (client/get "https://expired.badssl.com"))))

