(ns cliplater.core
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [khroma.log :as log]
   )
  )

(enable-console-print!)

(defonce app-state (atom {:text "clip later"}))

(defn ^:export root [data owner]
  (reify
    om/IRender
    (render [this]
      (html/html [:div (:text data)]))))

(defn ^:export run []
  (om/root root app-state
           {:target (. js/document (getElementById "container"))}))
