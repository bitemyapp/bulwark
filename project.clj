(defproject bulwark "0.0.3"
  :description "Defensive Ring middleware like Rack::attack."
  :url "http://github.com/bitemyapp/bulwark"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-time "0.5.1"]
                 [com.cemerick/pomegranate "0.2.0"]
                 [ring-mock "0.1.5"]
                 [com.taoensso/carmine "2.2.0"]])
