(defproject org.martinklepsch/clj-http-lite "0.4.3"
  :description "A Clojure HTTP library similar to clj-http, but more lightweight."
  :url "https://github.com/martinklepsch/clj-http-lite/"
  :license {:name "MIT"
            :url "http://www.opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [slingshot "0.12.2"]]
  :profiles {:dev {:dependencies [[ring/ring-jetty-adapter "1.3.2"]
                                  [ring/ring-devel "1.3.2"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}}
  :test-selectors {:default  #(not (:integration %))
                   :integration :integration
                   :all (constantly true)}
  :aliases {"all" ["with-profile" "dev,1.4:dev,1.5:dev,1.6:dev,1.7:dev,1.8:dev,1.9"]}
  :checksum-deps true)
