(ns cliplater.animate
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [cljs.core.async :as async
             :refer [<! >! chan close! timeout put! alts!]]
   [khroma.log :as log]
   ))
(defn animate [component & args]
  (fn [props owner opts]
    (reify
      om/IRender
      (render [_]
        (om/build component props {:opts opts})))))