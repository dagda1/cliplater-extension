(ns cliplater.core
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [khroma.log :as log]
   [khroma.tabs :as tabs]
   [khroma.runtime :as runtime]
   )
  )

(enable-console-print!)

(defonce app-state (atom {:text "clip later"}))

(defn ^:export root [data owner]
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
