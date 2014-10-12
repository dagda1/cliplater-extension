(ns cliplater.core
  (:require
   [om.core :as om :include-macros true]
   [clojure.walk :as walk]
   [cljs.core.async :as async]
   [om.dom :as dom :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [khroma.log :as log]
   [khroma.tabs :as tabs]
   [khroma.runtime :as runtime])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

(defonce app-state (atom {:text "clip later"}))

(defn get-active-tab []
  (let [ch (async/chan)]
    (.query js/chrome.tabs #js {:active true :currentWindow true}
      (fn [result]
        (when-let [tab (first result)]
          (async/put! ch (walk/keywordize-keys (js->clj {:tab tab})))))) ch))

(defn ^:export root [data owner]
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
    om/IRender
    (render [this]
      (html/html [:div.container#main
                  [:form.form-horizontal.clip-form
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
                             :value (om/get-state owner :url)
                             }]]]
                   [:div.control-group
                    [:div.controls
                     [:button.btn.btn-primary "Capture"]]]]]))))

(defn ^:export run []
  (om/root root app-state
           {:target (. js/document (getElementById "container"))}))
