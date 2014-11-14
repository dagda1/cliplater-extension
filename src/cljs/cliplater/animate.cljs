(ns cliplater.animate
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [cljs.core.async :as async
             :refer [<! >! chan close! timeout put! alts!]]
   [khroma.log :as log]))

(defn animate [cursor owner {:keys [build-fn id]}]
  (reify
    om/IDisplayName
    (display-name [_]
      "animate")
    om/IRender
    (render [this]
      build-fn)))
