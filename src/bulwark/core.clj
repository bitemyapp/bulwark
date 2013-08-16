(ns bulwark.core
  (:require [clj-time.core :as time]
            [bulwark.redis :as redis]))


(def config (atom {:whitelist []
                   :blacklist []
                   :track     []
                   :log       []
                   :throttle  []
                   :propagate []}))

(def has-stuff? (complement empty?))
(defn vals-not-empty [m]
  (filter
   (fn [[k v]]
     (has-stuff? v)) m))

(defn set-config! [config])

(defn now []
  "Wrapper in case it needs swapped out."
  (time/now))

(defn register [key rule-name rule-fn]
  (let [added-at (now)]
    (swap! config update-in [key] conj [rule-name rule-fn added-at])))

;; true means kosher. Don't be a derp.
(defn add-whitelist [rule-name rule-fn]
  (register :whitelist rule-name rule-fn))

(defn add-blacklist [rule-name rule-fn]
  (register :blacklist rule-name rule-fn))

(defn add-tracker [rule-name rule-fn]
  (register :track rule-name rule-fn))

(defn add-logger [logger-name log-fn]
  (register :log logger-name log-fn))

(defn add-throttle [throttle-name id limit period throttle-fn])

(def urls ["http://www.youtube.com/watch?v=a01QQZyl-_I"
           "http://www.youtube.com/watch?v=adrjxbVOtXk"])

(def throttle-response {:status 503
                        :headers {"Content-Type" "text/html"
                                  "Advice" "Slow your fucking roll"}
                        :body (str "<a href='" (rand-nth urls) "'>We're</a>")})

(def blacklist-response ;; or if the whitelist fails
  {:status 403
   :headers {"Content-Type" "text/plain"}
   :body "NOT. WELCOME."})

(defn test-list [req tests]
  (map (fn [[label rule-fn added-at]] (rule-fn req)) tests))

(defn sumtin-failed? [req tests]
  (some false? (test-list req tests)))

(defn protect-middleware [app]
  (fn [req]
    (let [cm @config
          maybe-work (vals-not-empty cm)
          work? (has-stuff? maybe-work)
          work (into {} maybe-work)
          whitelist (seq (:whitelist work))
          blacklist (seq (:blacklist work))
          throttle (seq (:throttle work))
          log (seq (:log work))]
          ;; track (seq (:track work))
          ;; propagate (seq (:propagate work))
      ;; track and propagate don't do anything.
      (when work?
        (when (or (and whitelist (sumtin-failed? req whitelist))
                  (sumtin-failed? req blacklist))
          blacklist-response)
        (when (sumtin-failed? req throttle)
          throttle-response)
        (app req)))))
        ;; (when log
        ;;   (test-list req log)
