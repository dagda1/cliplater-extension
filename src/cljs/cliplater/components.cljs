
(ns cliplater.components
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [khroma.log :as log])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop alt!]])
  (:use [cliplater.util :only [q]]))

(defn text-box [c owner {:keys [k label]}]
  (om/component
   (html/html
      [:div.form-group
       [:label.control-label {:htmlFor label} label]
       [:div.controls
        [:input.form-control {
                              :ref label
                              :name label
                              :value (k c)
                              :onChange #(om/update! c k (.. % -target -value))
                              }]]])))