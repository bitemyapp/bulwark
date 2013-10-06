(ns bulwark.util
  (:require [clj-time.core :as time]))

(defn epoch []
  (time/interval (time/epoch) (time/now)))

(defn epic-epoch []
  (time/in-seconds (epoch)))

(defn precise-epoch []
  (time/in-millis (epoch)))
