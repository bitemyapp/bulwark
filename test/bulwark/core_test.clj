(ns bulwark.core-test
  (:require [clojure.test :refer :all]
            [bulwark.core :refer :all]
            [fusillade.core :refer [burst]]
            [ring.mock.request :refer :all]))

(defn app [req] {:status 200 :body "kosher"})

(deftest test-exercise-with-mock
  (testing "Test blacklisting"
    (let [protect (protect-middleware app
                   {:blacklist [["nothing touches /untouchable"
                                 (fn [req] (not= (:uri req) "/untouchable"))]]})]
      (is (= (:status (protect (request :get "/untouchable")))
             403))
      (is (= (:status (protect (request :get "/okay")))
             200))))

  (testing "Test whitelisting"
    (let [protect (protect-middleware app
                   {:whitelist [["only /okay is allowed"
                                 (fn [req] (= (:uri req) "/okay"))]]})]
      (is (= (:status (protect (request :get "/okay")))
             200))
      (is (= (:status (protect (request :get "/notokay")))
             403))))

  (testing "Test throttling"
    (let [protect (protect-middleware app
                   {:throttle [["localhost is throttled" 3 10  (fn [req] (:remote-addr req))]]})]
      (is (= (:status (protect (request :get "/okay")))
             200)))))
