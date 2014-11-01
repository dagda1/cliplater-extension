(ns cliplater.core
  (:require
   [om.core :as om :include-macros true]
   [clojure.walk :as walk]
   [cljs.core.async :as async
             :refer [<! >! chan close! timeout sliding-buffer put! alts! pub sub unsub unsub-all]]
   [om.dom :as dom :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [khroma.log :as log]
   [khroma.tabs :as tabs]
   [khroma.runtime :as runtime])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop alt!]])
  (:use
   [cliplater.util :only [q]]))

(enable-console-print!)

(def app-state (atom {:clips []}))

(defn clips []
  (om/ref-cursor (:clips (om/root-cursor app-state))))

(defn get-active-tab []
  (let [ch (async/chan)]
    (.query js/chrome.tabs #js {:active true :currentWindow true}
      (fn [result]
        (when-let [tab (first result)]
          (async/put! ch (walk/keywordize-keys (js->clj {:tab tab})))))) ch))

(defn make-channels []
  {
   :save-clip (async/chan)
   })

(defn clip-view [clip]
  [:tr
   [:td
    [:a {:href (:url clip) :target "new"} (:title clip)]]
   [:td.delete
    [:a {:href "#"} "delete"]]])

(defn clips-view [data owner]
  (reify
    om/IWillMount
      (will-mount [_]
        (let [xs (clips)]
         (when-let [save-ch (om/get-shared owner [:channels :save-clip])]
           (go-loop []
             (when-let [clip (<! save-ch)]
               (do
                 (log/debug (str "received clip " clip))
                 (om/transact! xs #(vec (conj % clip)))
                 (recur)))))))
    om/IRender
    (render [this]
      (let [xs (om/observe owner (clips))]
       (html/html
        [:div.well
         [:table.table.table-bordered.table-hover.table-striped
          [:tbody
           (if (= (count xs) 0)
             [:tr
              [:td.text-center {:colSpan "2"} "No Clips!"]]
             (map clip-view xs))]]])))))

(defn capture-panel [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:title "loading..." :url "loading...."})
    om/IWillMount
      (will-mount [_]
        (let [ch (get-active-tab)]
          (go
            (let [{:keys [tab]} (<! ch)]
              (om/set-state! owner :title (:title tab))
              (om/set-state! owner :url (:url tab))))))
    om/IDidMount
    (did-mount [_]
      (.addEventListener (q "a.btn.btn-primary") "click"
                         (fn [e]
                           (when-let [ch (om/get-shared owner [:channels :save-clip])]
                             (async/put! ch {:title (om/get-state owner :title) :url (om/get-state owner :url)})))))
    om/IRender
    (render [this]
      (html/html [:div.form-horizontal.clip-form
                   [:legend "Capture Url"]
                   [:div.control-group
                    [:label.control-label {:for "title"} "title"]
                    [:div.controls
                     [:input.input-vlarge {
                             :name "title"
                             :value (om/get-state owner :title)}]]]
                   [:div.control-group
                    [:label.control-label {:for "url"} "url"]
                    [:div.controls
                     [:input.input-vlarge {
                             :name "url"
                             :value (om/get-state owner :url)}]]]
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
         (om/build clips-view data)))))

(defn ^:export run []
  (let [channels (make-channels)]
   (om/root root app-state
            {:target (. js/document (getElementById "container")) :shared {:channels channels} } ) channels ))
