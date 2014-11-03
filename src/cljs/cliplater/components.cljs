(ns cliplater.components
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [cljs.core.async :as async
             :refer [<! >! chan close! timeout sliding-buffer put! alts! pub sub unsub unsub-all tap mult]]
   [khroma.log :as log])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop alt!]])
  )

(defn text-box [state owner {:keys [value]}]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [ch (async/chan)]
        (tap (om/get-shared owner [:tab-mult]) ch)
        (go-loop []
          (when-let [tab (<! ch)]
            (om/set-state! owner value (value tab))
            (recur)))))
    om/IRender
    (render [this]
      (let [label (name value)]
       (html/html
        [:div.form-group
         [:label.control-label {:htmlFor label} label]
         [:div.controls
          [:input.form-control {
                                :name label
                                :value (om/get-state owner value)}]]])))))