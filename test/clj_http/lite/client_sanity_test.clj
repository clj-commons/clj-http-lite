(ns clj-http.lite.client-sanity-test
  "A small subset of tests suitable for sanity testing.
  Used by babashka libs tests."
  (:require [clj-http.lite.client :as client]
            [clojure.test :as t :refer [deftest is]]))

(deftest client-test
  (is (= 200 (:status (client/get "https://www.clojure.org" {:throw-exceptions false}))))

  (is (= 200 (:status (client/get "https://postman-echo.com/get?foo1=bar1&foo2=bar2" {:throw-exceptions false}))))

  (is (= 200 (:status (client/post "https://postman-echo.com/post" {:throw-exceptions false}))))

  (is (= 200 (:status (client/post "https://postman-echo.com/post"
                                   {:body "{\"a\": 1}"
                                    :headers {"X-Hasura-Role" "admin"}
                                    :content-type :json
                                    :accept :json
                                    :throw-exceptions false}))))

  (is (= 200  (:status (client/put "https://postman-echo.com/put"
                                   {:body "{\"a\": 1}"
                                    :headers {"X-Hasura-Role" "admin"}
                                    :content-type :json
                                    :accept :json
                                    :throw-exceptions false})))))

(deftest exception-test
  (try (client/get "https://httpbin.org/status/404")
       (is false "should not reach here")
       (catch Exception e
         (is (:headers (ex-data e))))))

(deftest insecure-test
  (is (thrown? Exception
               (client/get "https://expired.badssl.com")))
  (is (= 200 (:status (client/get "https://expired.badssl.com" {:insecure? true}))))
  (is (thrown? Exception
               (client/get "https://expired.badssl.com"))))
