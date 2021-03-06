(ns bulwark.core
  (:require [clj-time.core :as time]
            [bulwark.redis :as redis]
            [bulwark.util :refer [epic-epoch]]))

;; 403 and 503 handlers

(defn rand-vid []
  (let [vids ["a01QQZyl-_I" "adrjxbVOtXk"]]
    (rand-nth vids)))

(defn throttle-response [req]
  {:status 503
   :headers {"Content-Type" "text/html"
             "Advice" "Slow your fucking roll"}
   :body (str "<iframe width=\"420\"
               height=\"315\"
               src=\"//www.youtube.com/embed/"
               (rand-vid)
               "\" frameborder=\"0\" allowfullscreen></iframe>")})

(defn blacklist-response [req] ;; or if the whitelist fails
  {:status 403
   :headers {"Content-Type" "text/plain"}
   :body "NOT. WELCOME."})

;; Default and global config, can be overridden by closure.
(def config (atom {:whitelist []
                   :blacklist []
                   :track     []
                   :log       []
                   :throttle  []
                   :propagate []
                   :handlers  {:503 throttle-response
                               :403 blacklist-response}
                   :record-hit redis/record-hit
                   :query-hits redis/query-hits}))

(def has-stuff? (complement empty?))
(defn vals-not-empty [m]
  (let [seqs-only (filter (fn [[k v]] (vector? v)) m)]
    (filter
     (fn [[k v]]
       (has-stuff? v)) seqs-only)))

(defn now []
  "Wrapper in case it needs swapped out."
  (time/now))

(defn register [key rule-name rule-fn]
  (let [added-at (now)]
    (swap! config update-in [key] conj [rule-name rule-fn added-at])))

;; true means kosher. Don't be a derp.
(defn whitelist [rule-name rule-fn]
  (register :whitelist rule-name rule-fn))

(defn blacklist [rule-name rule-fn]
  (register :blacklist rule-name rule-fn))

(defn track [rule-name rule-fn]
  (register :track rule-name rule-fn))

(defn log [logger-name log-fn]
  (register :log logger-name log-fn))

;; give me a keyword back for a request with the id-fn.
(defn throttle [throttle-name limit period id-fn]
  (swap! config update-in [:throttle] conj [throttle-name limit period id-fn]))

(defn test-list [req tests]
  (map (fn [[label rule-fn added-at]] (rule-fn req)) tests))

(defn sumtin-failed? [req tests]
  (some false? (test-list req tests)))

(defn throttle-id? [throttled query-hits]
  {:pre  [(fn? query-hits)]}
  (let [now (epic-epoch)]
    (some true?
          (map (fn [[id name limit period id-fn]]
                 (let [past (- now period)
                       queried (query-hits id past now)
                       accessed (count queried)]
                   (>= accessed limit)))
               throttled))))

(defn protect-middleware [app & [closure-config]]
  (fn [req]
    (let [cc (or closure-config {})
          cm (merge @config cc)
          ip (:remote-addr req)
          record-hit (:record-hit cm)
          query-hits (:query-hits cm)
          maybe-work (vals-not-empty cm)
          ;; work? and work are how we know whether any whitelist,
          ;; blacklist, or throttling rules are active.
          work? (has-stuff? maybe-work)
          work (into {} maybe-work)
          ;; We break out the "work", each being a rule in the
          ;; whitelist, blacklist, and throttle categories
          whitelist (seq (:whitelist work))
          blacklist (seq (:blacklist work))
          throttle (seq (:throttle work))
          ;; These aren't used as much at this time.
          log (seq (:log work))
          track (seq (:track work))
          propagate (seq (:propagate work))
          ;; "id" is a genericization of identifying an originator
          ;; of requests. This can be IP Address, user email, or anything.
          throttle-with-id (map (fn [[name limit period id-fn]]
                                  [(id-fn req) name limit period id-fn])
                                throttle)
          throttled (set (map #(first %)
                              throttle-with-id))
          reject-blacklisted (and work?
                                  (or (and whitelist
                                           (sumtin-failed? req whitelist))
                                      (sumtin-failed? req blacklist)))
          reject-throttled (and work? throttle
                                (throttle-id? throttle-with-id query-hits))
          throttle-handler ((cm :handlers) :503)
          blacklist-handler ((cm :handlers) :403)
          response (or (and reject-blacklisted
                            (blacklist-handler req))
                       (and reject-throttled
                            (throttle-handler req))
                       (app req))]
      ;; track and propagate don't do anything. More coffee plz.
      (doseq [id throttled]
        (record-hit id req))
      response)))
