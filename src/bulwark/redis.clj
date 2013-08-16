(ns bulwark.redis
  (:require [taoensso.carmine :as car :refer (wcar)]
            [bulwark.util :refer [epic-epoch]]))

;;  Override if you want.
(def redis-conn {:pool {} :spec {:host "127.0.0.1" :port 6379}})
(def namespace "bulwark")

;;  `conn` arg is a map with connection pool and spec options:
;;    {:pool {} :spec {:host "127.0.0.1" :port 6379}} ; Default
;;    {:pool {} :spec {:uri "redis://redistogo:pass@panga.redistogo.com:9475/"}}
;;    {:pool {} :spec {:host "127.0.0.1" :port 6379
;;                     :password "secret"
;;                     :timeout-ms 6000
;;                     :db 3}}
;;  A `nil` or `{}` `conn` or opts will use defaults. A `:none` pool can be used
;;  to skip connection pooling. For other pool options, Ref. http://goo.gl/EiTbn.

(defmacro wcar* [& body] `(car/wcar redis-conn ~@body))

(defn key-for-id [id]
  (str namespace ":" (name id)))

(defn record-hit [id data]
  "id should be a :keyword used to identify or discretize
   requests, data should be a map. Like a Ring request,
   a user/session id, or an IP address.
   {:ip \"127.0.0.1\" :session 234 :user 0}"
  (wcar* (car/zadd (key-for-id id) (epic-epoch) data)))

(defn query-hits [id start stop]
  "Pass me in-seconds Unix time or die."
  (wcar* (car/zrange (key-for-id id) start stop)))
