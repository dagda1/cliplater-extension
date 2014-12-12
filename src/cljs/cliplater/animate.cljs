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

;; find any deleted keys and add them into the correct slot
;; After 1 item added
;; childMapping {:new_item_id, element}
;; previousKeysPending {}
;; nextKeysPending {}
;; pendingKeys: []
;; var pendingKeys = [];
;; for (var prevKey in prev) {
;;   if (next.hasOwnProperty(prevKey)) {
;;     if (pendingKeys.length) {
;;       nextKeysPending[prevKey] = pendingKeys;
;;       pendingKeys = [];
;;     }
;;   } else {
;;     pendingKeys.push(prevKey);
;;   }
;; }
;; var i;
;; var childMapping = {};
;; for (var nextKey in next) {
;;   if (nextKeysPending.hasOwnProperty(nextKey)) {
;;     for (i = 0; i < nextKeysPending[nextKey].length; i++) {
;;       var pendingNextKey = nextKeysPending[nextKey][i];
;;       childMapping[nextKeysPending[nextKey][i]] = getValueForKey(
;;         pendingNextKey
;;       );
;;     }
;;   }
;;   childMapping[nextKey] = getValueForKey(nextKey);
;; }

;; // Finally, add the keys which didn't appear before any key in `next`
;; for (i = 0; i < pendingKeys.length; i++) {
;;   childMapping[pendingKeys[i]] = getValueForKey(pendingKeys[i]);
;; }

;; return childMapping;
(defn mergeChildMappings [prevChildMapping nextChildMapping]
  (let [prev (or prevChildMapping (js-obj))
        next (or nextChildMapping (js-obj))
        prevKeys (.keys js/Object prev)
        nextKeys (.keys js/Object next)
        nextKeysPending (js-obj)]
    (letfn [(getValueForKey [key]
              (log "next" (aget next key))
              (if (.hasOwnProperty next key)
                (aget next key)
                (aget prev key)))]
      ;(log "prev" prev)
      ;(log "next" next)
      (let [child-mapping (reduce (fn [acc key]
                                    (assoc acc key (getValueForKey key))
                                    ) {} nextKeys )]
        (log "child-mapping" (clj->js child-mapping))
        (clj->js child-mapping)
        )
      )))

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
       (let [children (.. this -state -children)
             keys (.keys js/Object children)
             len (alength keys)]
         (dotimes [i len]
           (let [component (aget (.-refs this) (aget keys i))
                 node (.getDOMNode component)
                 className (.-className node)]
             (.setTimeout js/window #(do
                                       (addClass node "in")
                                       ) 150))))))

    :componentWillReceiveProps
    (fn [nextProps]
      (this-as this
               (let [prevChildMapping (.. this -state -children)
                     nextChildren (.-children nextProps)
                     nextChildMapping (js/React.Children.map nextChildren (fn [child] child))
                     mergedChildMappings (mergeChildMappings prevChildMapping nextChildMapping)
                     mergedKeys (.keys js/Object mergedChildMappings)]

                 (.setState this #js {:children mergedChildMappings}))))
    :render
    (fn []
      (this-as this
               (let [children (js->clj (.. this -state -children) :keywordize-keys true)
                     childrenToRender (clj->js (into {} (for [[k v] children]
                                                  (let [key (.-name k)]
                                                    {key (clone-with-props v {:ref key} )}))))]
                 (js/React.DOM.tbody nil childrenToRender))))}))