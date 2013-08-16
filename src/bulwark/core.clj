(ns bulwark.core
  (:require [clj-time.core :as time]
            [bulwark.redis :refer [record-hit query-hits]]))


(def config (atom {:whitelist []
                   :blacklist []
                   :track     []
                   :log       []
                   :throttle  []
                   :propagate []}))

(defn set-config! [config])

(defn now []
  "Wrapper in case it needs swapped out."
  (time/now))

(defn register [key rule-name rule-fn]
  (let [added-at (now)]
    (swap! config update-in [key] conj [rule-name rule-fn added-at])))

(defn add-whitelist [rule-name rule-fn]
  (register :whitelist rule-name rule-fn))

(defn add-blacklist [rule-name rule-fn]
  (register :blacklist rule-name rule-fn))

(defn add-tracker [rule-name rule-fn]
  (register :track rule-name rule-fn))

(defn add-logger [logger-name log-fn]
  (register :log logger-name log-fn))

(defn add-throttle [throttle-name id limit period throttle-fn])

(defn protect-middleware [app]
  (fn [req]
    ;; no-op
    (app req)))
