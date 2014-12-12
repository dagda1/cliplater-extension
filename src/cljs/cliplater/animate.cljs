(ns cliplater.animate
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [cljs.core.async :as async
             :refer [<! >! chan close! timeout put! alts!]]
   [clojure.walk :as walk]
   [goog.object :as gobject]
   [khroma.log :as log])
  (:use
   [cliplater.util :only [q guid addClass log]]))

(defprotocol IHandleDoneEntering
  (handle-done-entering [key]))

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

(defn object-keys [o]
  (clj->js ((fn [obj]
                    (filter (fn [k]
                              (. obj (hasOwnProperty k))
                              ) (js-keys next))) o)))

(defn clone-with-props [child props]
  (let [new-obj (clone-obj (clj->js props))
        child-props (.-props child)]

    (merge-props! new-obj child-props)

    (when (and (.hasOwnProperty child-props "children") (not (.hasOwnProperty props "children")))
      (aset new-obj "children" (.-children child-props)))

    ((.-constructor child) new-obj)))

(defn mergeChildMappings [prevChildMapping nextChildMapping]
  (let [prev (or prevChildMapping (js-obj))
        next (or nextChildMapping (js-obj))
        prevKeys (.keys js/Object prev)
        nextKeys (.keys js/Object next)
        nextKeysPending (js-obj)]

    (letfn [(getValueForKey [key]
              (if (.hasOwnProperty next key)
                (aget next key)
                (aget prev key)))]

      (let [pendingKeys (reduce (fn [acc key]
                                  (conj acc key)
                                  ) [] prevKeys)

            child-mapping (reduce (fn [acc key]
                                    (assoc acc key (getValueForKey key))
                                    ) {} (into pendingKeys (js->clj nextKeys)))]

        (clj->js child-mapping)))))

(def animate
  (js/React.createClass
   #js
   {:getInitialState
    (fn []
      (this-as this
               {:children (js-obj)}))
    :componentDidUpdate
    (fn []
      (this-as this
       (let [keysToEnter (aget this "keysToEnter")
             len (alength keysToEnter)]

         (dotimes [i len]
           (let [component (aget (.-refs this) (aget keysToEnter i))
                 node (.getDOMNode component)
                 className (.-className node)]
             (.setTimeout js/window #(addClass node "in") 150))))))

    :componentWillReceiveProps
    (fn [nextProps]
      (this-as this
               (let [prevChildMapping (.. this -state -children)
                     nextChildMapping (js/React.Children.map (.-children nextProps) (fn [child] child))
                     mergedChildMappings (mergeChildMappings prevChildMapping nextChildMapping)
                     prevKeys (.keys js/Object (or prevChildMapping (js-obj)))
                     nextKeys (.keys js/Object (or nextChildMapping (js-obj)))]

                 (.setState this #js {:children mergedChildMappings})

                 (let [keysToEnter (clj->js (filter #(not (.hasOwnProperty prevChildMapping %)) (js->clj nextKeys)))
                       keysToLeave (clj->js (filter #(not (.hasOwnProperty nextChildMapping %)) (js->clj prevKeys)))]

                   (set! (.-keysToEnter this) (clj->js keysToEnter))
                   (set! (.-keysToLeave this) (clj->js keysToLeave))))))
    :render
    (fn []
      (this-as this
               (let [children (js->clj (.. this -state -children) :keywordize-keys true)
                     childrenToRender (clj->js (into {} (for [[k v] children]
                                                  (let [key (.-name k)]
                                                    {key (clone-with-props v {:ref key} )}))))]
                 (js/React.DOM.tbody nil childrenToRender))))}))