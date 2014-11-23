(ns cliplater.animate
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [cljs.core.async :as async
             :refer [<! >! chan close! timeout put! alts!]]
   [clojure.walk :as walk]
   [khroma.log :as log]))

(defn clone-object [key obj]
  (goog.object/forEach obj (fn [val key obj]
                                            (log/debug key))))

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
                 (js->clj :keywordize-keys true))}))
    :render
    (fn []
      (this-as this
               (let [children (:children (.. this -state))]
                 (doseq [[k v] children]
                   (clone-object k v)))))}))
