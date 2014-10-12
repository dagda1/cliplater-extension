(ns cliplater.core
  (:require
   [om.core :as om :include-macros true]
   [clojure.walk :as walk]
   [cljs.core.async :as async]
   [om.dom :as dom :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [khroma.log :as log]
   [khroma.tabs :as tabs]
   [khroma.runtime :as runtime]
   )
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]
   )
  )

(enable-console-print!)

(defonce app-state (atom {:text "clip later"}))

(defn get-active-tab []
  (let [ch (async/chan)]
    (.query js/chrome.tabs #js {:active true :currentWindow true}
      (fn [result]
        (when-let [tab (first result)]
          (async/put! ch (walk/keywordize-keys (js->clj {:tab tab})))))) ch))

(defn ^:export root [data owner]
  (let [ch (get-active-tab)]
    (go
      (let [{:keys [tab]} (<! ch)]
        (log/debug (:url tab))
        )
      )
    )

  (reify
    om/IRender
    (render [this]
      (html/html [:div.container#main
                  [:div.controls.text-center
                   [:button.btn.btn-primary "Capture"]
                   ]
                  ]))))

(defn ^:export run []
  (om/root root app-state
           {:target (. js/document (getElementById "container"))}))
