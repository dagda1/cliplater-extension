(ns cliplater.animate
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [cljs.core.async :as async
             :refer [<! >! chan close! timeout put! alts!]]
   [clojure.walk :as walk]
   [goog.object :as gobject]
   [khroma.log :as log]))

(defn merge-props! [obj props]
  (goog.object/forEach props (fn [v k o]
                               (when (and (.hasOwnProperty o k)
                                          (not= k "children")
                                          (not= k "ref"))
                                (aset obj k v)))))

(defn clone-obj [obj]
  (let [new-obj (js-obj)]
    (goog.object/forEach obj (fn [v k o]
                               (when (.hasOwnProperty o k)
                                 (aset new-obj k v))))
    new-obj))

(defn clone-with-props [child props]
  (let [new-obj (clone-obj (clj->js props))
        child-props (.-props child)]

   (merge-props! new-obj child-props)

   (when (and (.hasOwnProperty child-props "children") (not (.hasOwnProperty props "children")))
     (aset new-obj "children" (.-children child-props)))

   new-obj))

(def animate
  (js/React.createClass
   #js
   {:getInitialState
    (fn []
      (this-as this
               (log/debug (.. this -props -children))
               {:children
                (->
                 (.. this -props -children)
                 (js/React.Children.map (fn [child] child))
                 (js->clj :keywordize-keys true))}))
    :render
    (fn []
      (this-as this
               (let [children (:children (.. this -state))
                     childrenToRender (into {} (for [[k v] children]
                                                 (let [key (.-name k)]
                                                   {key (clone-with-props v {:ref key} )})))]
                 (let [result (dom/td nil childrenToRender)]
                   result))))}))