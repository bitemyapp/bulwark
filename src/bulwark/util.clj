(ns bulwark.util
  (:require [clj-time.core :as time]))

(defn epic-epoch []
  (time/in-secs (time/interval (time/epoch) (time/now))))
