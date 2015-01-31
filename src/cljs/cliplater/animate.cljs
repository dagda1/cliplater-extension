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
   [cliplater.util :only [q guid addClass log removeClass]]))

(def TICK 17)

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

    (js/React.createElement (.type child) new-obj)))

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

(defn performEnter [node]
  (.setTimeout js/window #(addClass node "in") TICK))

(defn handleDoneLeaving [key]
  (this-as this
           (let [children (.. this -state -children)]
             (js-delete children key)
             (.setState this #js {:children children} ))))

(defn performLeave [node key]
  (this-as this
           (.addEventListener node "transitionend" (.bind handleDoneLeaving this key) false))

  (.setTimeout js/window #(removeClass node "in") TICK))

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
             keysToEnterLen (alength keysToEnter)
             keysToLeave (aget this "keysToLeave")
             keysToLeaveLen (alength keysToLeave)]

         (dotimes [i keysToEnterLen]
           (let [key (aget keysToEnter i)
                 component (aget (.-refs this) key)
                 node (.getDOMNode component)]
             (performEnter node)))

         (set! (.-keysToEnter this) [])

         (dotimes [i keysToLeaveLen]
          (let [key (aget keysToLeave i)
                component (aget (.-refs this) key)
                node (.getDOMNode component)]
            ((.bind performLeave this) node key)))

         (set! (.-keysToLeave this) []))))

    :componentWillReceiveProps
    (fn [nextProps]
      (this-as this
               (let [prevChildMapping (.. this -state -children)
                     nextChildMapping (or (js/React.Children.map (.-children nextProps) (fn [child] child)) (js-obj))
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
               (let [children (.. this -state -children)
                     childrenToRender  (js-obj)]
                 (goog.object/forEach children (fn [v k o]
                                                 (aset childrenToRender k (clone-with-props v {:ref k}))))
                 (log "childrentorender" childrenToRender)
                 (js/React.DOM.tbody nil childrenToRender))))}))
