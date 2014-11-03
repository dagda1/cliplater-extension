(ns cliplater.core
  (:require
   [om.core :as om :include-macros true]
   [clojure.walk :as walk]
   [cljs.core.async :as async
             :refer [<! >! chan close! timeout sliding-buffer put! alts! pub sub unsub unsub-all tap mult]]
   [om.dom :as dom :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [khroma.log :as log]
   [khroma.tabs :as tabs]
   [khroma.runtime :as runtime]
   [cliplater.components :as ui])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop alt!]])
  (:use
   [cliplater.util :only [q]]))

(enable-console-print!)

(def app-state (atom {:clips [] :current-tab {:title "loading..." :url "loading...."}}))

(defn get-active-tab []
  (let [ch (async/chan)]
    (.query js/chrome.tabs #js {:active true :currentWindow true}
      (fn [result]
        (when-let [tab (first result)]
          (async/put! ch (walk/keywordize-keys (js->clj {:tab tab})))))) ch))

(defn clip-view [clip]
  [:tr
   [:td
    [:a {:href (:url clip) :target "new"} (:title clip)]]
   [:td.delete
    [:a {:href "#"} "delete"]]])

(defn clips-view [clips]
  (reify
    om/IRender
    (render [this]
      (html/html
        [:div.well
         [:table.table.table-bordered.table-hover.table-striped
          [:tbody
           (if (empty? clips)
             [:tr
              [:td.text-center {:colSpan "2"} "No Clips!"]]
             (map clip-view clips))]]]))))

(defn capture-panel [{clips :clips {:keys [title url] :as current-tab} :current-tab}]
  (reify
    om/IInitState
    (init-state [_]
      {:title "loading..." :url "loading...."})
    om/IWillMount
      (will-mount [_]
        (let [ch (get-active-tab)]
          (go-loop []
            (let [{:keys [tab]} (<! ch)]
              (om/update! current-tab tab)
              (recur)))))
    om/IDidMount
    (did-mount [_]
      (.addEventListener (q "a.btn.btn-primary") "click"
                         #(om/transact! clips (fn [clips] (conj clips @current-tab)))))
    om/IRender
    (render [this]
      (html/html [:div.clip-form
                   [:legend "Capture Url"]
                   (om/build ui/text-box title {:opts {:label "title"}})
                   (om/build ui/text-box url {:opts {:label "value"}})
                   [:div.control-group
                    [:div.controls
                     [:a.btn.btn-primary  {:href "#"} "Capture"]]]]))))

(defn ^:export root [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:id "main" :className "container row-fluid"}
        (dom/div nil
         (om/build capture-panel data))
         (om/build clips-view (:clips data))))))

(defn ^:export run []
  (om/root root app-state
            {:target (. js/document (getElementById "container")) }))
