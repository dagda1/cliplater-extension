
(ns cliplater.components
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [khroma.log :as log])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop alt!]])
  )

(defn text-box [value _ {:keys [label]}]
  (om/component
   (html/html
        [:div.form-group
         [:label.control-label {:htmlFor label} label]
         [:div.controls
          [:input.form-control {
                                :name label
                                :value value}]]])))