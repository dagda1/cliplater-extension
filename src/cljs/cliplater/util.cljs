(ns cliplater.util
  (:require
   [khroma.log :as log]
   [clojure.string :as str])
  (:import [goog.ui IdGenerator]))

(defn log [key obj]
  (log/debug "=======================")
  (log/debug key)
  (.dir js/console obj)
  (log/debug key)
  (log/debug "======================="))

(defn guid []
  (.getNextUniqueId (.getInstance IdGenerator)))

(defn q [selector]
  (.querySelector js/document selector))

(defn hasClass [node className]
  (let [existing (.-className node)]
    (re-find (re-pattern (str "\\b" className "\\b")) existing)))

(defn addClass [node className]
  (let [existing (.-className node)]
    (when-not (hasClass node className)
      (aset node "className" (str existing " " className)))))

(defn removeClass [node className]
  (let [existing (.-className node)]
    (when (hasClass node className)
      (aset node "className" (str/replace existing (re-pattern (str ".?\\b" className "\\b")) "")))))