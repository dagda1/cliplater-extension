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
               {:children (js/React.Children.map (.. this -props -children) (fn [child] child))}))
    :render
    (fn []
      (this-as this
               (let [children (js->clj (:children (.. this -state)) :keywordize-keys false)]
                 (log/debug children)
                 (doseq [k (keys children)]
                   (log/debug k)))))}))