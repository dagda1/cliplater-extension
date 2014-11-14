(ns cliplater.components
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [cljs.core.async :as async
             :refer [<! >! chan close! timeout sliding-buffer put! alts! pub sub unsub unsub-all tap mult]]
   [khroma.log :as log])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop alt!]])
  (:use [cliplater.util :only [q guid]]))

(def ENTER_KEY 13)

(defn text-box [c owner {:keys [k label needs-focus]}]
  (reify
    om/IInitState
    (init-state [_]
      {:event-channel (om/get-shared owner [:channels :event-channel])})
    om/IDisplayName
    (display-name [_]
      "text-box")
    om/IDidMount
    (did-mount [_]
      (when needs-focus
        (do
          (let [node (om/get-node owner label)
                len (.. node -value -length)]
            (.focus node)))))
    om/IRender
    (render [this]
      (dom/div #js {:className "form-group"}
               (dom/label #js {:className "control-label" :htmlFor label} label)
               (dom/div #js {:className "controls"}
                        (dom/input #js {:className "form-control"
                                        :type "text"
                                        :ref label
                                        :name label
                                        :value (k c)
                                        :onKeyDown (fn [e]
                                                     (when (== (.-which e) ENTER_KEY)
                                                       (let [new-clip {:id (guid) :title (:title @c) :url (:url @c)}]
                                                         (put! (om/get-state owner :event-channel) [:save new-clip]))))
                                        :onChange #(om/update! c k (.. % -target -value))}))))))