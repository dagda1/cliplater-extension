(ns cliplater.animate
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [cljs.core.async :as async
             :refer [<! >! chan close! timeout put! alts!]]
   [khroma.log :as log]
   ))

(defn animate [cursor owner {:keys [build-fn id]}]
  (reify
      om/IRender
      (render [this]
        (html/html
         build-fn))))
