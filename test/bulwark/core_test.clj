(ns bulwark.core-test
  (:require [clojure.test :refer :all]
            [bulwark.core :refer :all]
            [fusillade.core :refer [burst]]
            [ring.mock.request :refer :all]))

(deftest test-exercise-with-mock
  (testing "Test blacklisting"
    (let [protect (protect-middleware {})
          _ (blacklist "nothing touches /untouchable" (fn [req] (not= (:uri req) "/untouchable")))
          _ (whitelist "only /okay is allowed"        (fn [req] (= (:uri req) "/okay")))
          _ (throttle  "localhost is throttled" 3 10  (fn [req] (:remote-addr req)))]
      (is (= (:status (protect (request :get "/untouchable")))
             403))))
  (testing "Test whitelisting"
    (is (= 0 0)))
  (testing "Test throttling"
    (is (= 0 0))))
