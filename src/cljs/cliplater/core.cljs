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

(defn destroy-clip [data clip]
  (om/transact! data :clips (fn [clips] (into [] (remove #(= % clip) clips)))))

(defn handle-event [type data clip]
  (case type
    :destroy (destroy-clip data clip)
    ))

(defn make-channels []
  {
   :event-channel (chan)
   })

(defn clip-view [clip owner]
  (om/component
   (let [comm (om/get-shared owner [:channels :event-channel])]
    (html/html
     [:tr
      [:td
       [:a {:href (:url clip) :target "new"} (:title clip)]]
      [:td.delete
       [:a.btn.btn-danger {:href "#"
                           :ref "delete-clip"
                           :onClick #(put! comm [:destroy @clip])
                           } "delete"]]]))))

(defn clips-view [{:keys [clips]} owner]
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
             (om/build-all clip-view clips))]]]))))

(defn capture-panel [{clips :clips {:keys [title url] :as current-tab} :current-tab} owner]
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
    om/IRender
    (render [this]
      (html/html [:div.clip-form
                   [:legend "Capture Url"]
                   (om/build ui/text-box current-tab {:opts {:label "title" :k :title}})
                   (om/build ui/text-box current-tab {:opts {:label "url" :k :url}})
                   [:div.control-group
                    [:div.controls
                     [:a.btn.btn-primary {
                                          :ref "copy-clip"
                                          :href "#"
                                          :onClick (fn [e]
                                                     (let [input (q "input[name=url]")]
                                                       (set! (.-value input) (:url @current-tab))
                                                       (.select input)
                                                       (.execCommand js/document "copy")))
                                          } "Copy"]
                     [:a.btn.btn-success  {
                                           :ref "new-clip"
                                           :href "#"
                                           :onClick #(om/transact! clips (fn [clips] (conj clips {:title (:title @current-tab) :url (:url @current-tab)})))
                                           } "Capture"]]]]))))

(defn ^:export root [data owner]
  (reify
    om/IWillMount
    (will-mount [this]
      (let [comm (om/get-shared owner [:channels :event-channel])]
        (om/set-state! owner :comm comm)
          (go-loop []
            (let [[type clip] (<! comm)]
              (handle-event type data clip)
              )
            )
          )
      )
    om/IRender
    (render [this]
      (dom/div #js {:id "main" :className "container row-fluid"}
        (dom/div nil
         (om/build capture-panel data))
         (om/build clips-view data)))))

(defn ^:export run []
  (let [channels (make-channels)]
   (om/root root app-state
            {:target (. js/document (getElementById "container")) :shared {:channels channels} })))
