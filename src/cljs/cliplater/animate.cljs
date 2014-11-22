(ns cliplater.animate
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [cljs.core.async :as async
             :refer [<! >! chan close! timeout put! alts!]]
   [clojure.walk :as walk]
   [khroma.log :as log]))

(def animate
  (js/React.createClass
   #js
   {:getInitialState
    (fn []
      (this-as this
               {:children
                (->
                 (.. this -props -children)
                 (js/React.Children.map (fn [child] child))
                 (js->clj :keywordize-keys false))}))
    :render
    (fn []
      (this-as this
               (let [children (:children (.. this -state))]
                 (log/debug children)
                 (doseq [k (keys children)]
                   (log/debug k)))))}))
