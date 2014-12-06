(ns cliplater.animate
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [cljs.core.async :as async
             :refer [<! >! chan close! timeout put! alts!]]
   [clojure.walk :as walk]
   [goog.object :as gobject]
   [khroma.log :as log]))

(defn log [key obj]
  (log/debug "=======================")
  (log/debug key)
  (.dir js/console obj)
  (log/debug key)
  (log/debug "======================="))

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

;; function getValueForKey(key) {
;;   if (next.hasOwnProperty(key)) {
;;     return next[key];
;;   } else {
;;     return prev[key];
;;   }
;; }
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
;;
;; for (i = 0; i < pendingKeys.length; i++) {
;;   childMapping[pendingKeys[i]] = getValueForKey(pendingKeys[i]);
;; }
;
(defn mergeChildMappings [prevChildMapping nextChildMapping]
  (let [prev (or prevChildMapping (js-obj))
        next (or nextChildMapping (js-obj))
        ]
    next
    ))

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

    :componentDidMount
    (fn []
      (this-as this
               ; (log "DOM NODE" (.-outerHTML (.getDOMNode this)))
               )
      )
    :componentDidUpdate
    (fn []
     )
    :componentWillReceiveProps
    (fn [nextProps]
      (this-as this
               (let [prevChildMapping (.. this -state -children)
                     children (.-children nextProps)
                     nextChildMapping (js/React.Children.map children (fn [child] child))
                     mergedChildMappings (mergeChildMappings prevChildMapping nextChildMapping)
                     mergedKeys (.keys js/Object mergedChildMappings)
                     ]
                 (.setState this #js {:children mergedChildMappings}))))
    :render
    (fn []
      (this-as this
               (let [children (js->clj (.. this -state -children) :keywordize-keys true)
                     childrenToRender (clj->js (into {} (for [[k v] children]
                                                  (let [key (.-name k)]
                                                    {key (clone-with-props v {:ref key} )}))))]
                 (log "childrenToRender" childrenToRender)
                 (js/React.DOM.tbody nil childrenToRender))))}))