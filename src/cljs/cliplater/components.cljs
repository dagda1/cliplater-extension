(ns cliplater.components
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [cljs.core.async :as async
             :refer [<! >! chan close! timeout sliding-buffer put! alts! pub sub unsub unsub-all tap mult]]
   [sablono.core :as html :refer-macros [html]]
   [khroma.log :as log])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop alt!]])
  (:use [cliplater.util :only [q guid]]))

(def ENTER_KEY 13)

(defn text-box [c owner {:keys [k label needs-focus]}]
  (reify
    om/IDidMount
    (did-mount [_]
      (when needs-focus
        (do
          (let [node (om/get-node owner label)
                len (.. node -value -length)]
            (.focus node)))))
    om/IRender
    (render [this]
      (let [comm (om/get-shared owner [:channels :event-channel])]
        (html/html
         [:div.form-group
          [:label.control-label {:htmlFor label} label]
          [:div.controls
           [:input.form-control {
                               :ref label
                               :name label
                               :value (k c)
                               :onKeyDown (fn [e]
                                            (when (== (.-which e) ENTER_KEY)
                                              (let [new-clip {:id (guid) :title (:title @c) :url (:url @c)}]
                                                (put! comm [:save new-clip]))))
                               :onChange #(om/update! c k (.. % -target -value))}]]])))))