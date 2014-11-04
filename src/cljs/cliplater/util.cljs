(ns cliplater.util
  (:import [goog.ui IdGenerator]))

(defn guid []
  (.getNextUniqueId (.getInstance IdGenerator)))

(defn q [selector]
  (.querySelector js/document selector))