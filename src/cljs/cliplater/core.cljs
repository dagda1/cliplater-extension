(ns cliplater.core
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [clojure.walk :as walk]
   [cljs.core.async :as async
             :refer [<! >! chan close! timeout put! alts!]]
   [khroma.log :as log]
   [khroma.tabs :as tabs]
   [khroma.runtime :as runtime]
   [cliplater.components :as ui])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop alt!]])
  (:use
   [cliplater.util :only [q guid]]
   [cliplater.animate :only [animate]]))

(enable-console-print!)

(def app-state (atom {:clips [] :current-tab {:title "loading..." :url "loading...."}}))

(defn get-active-tab []
  (let [ch (async/chan)]
    (.query js/chrome.tabs #js {:active true :currentWindow true}
      (fn [result]
        (when-let [tab (first result)]
          (async/put! ch (walk/keywordize-keys (js->clj {:tab tab})))))) ch))

(defn save-clip [data new-clip]
  (om/transact! data :clips (fn [clips] (conj clips new-clip))))

(defn destroy-clip [data clip]
  (om/transact! data :clips (fn [clips] (into [] (remove #(= (:id %) (:id clip)) clips)))))

(defn handle-event [type data clip]
  (case type
    :save (save-clip data clip)
    :destroy (destroy-clip data clip)))

(defn make-channels []
  {:event-channel (chan)})

(defn clip-view [clip owner]
  (reify
   om/IInitState
   (init-state [_]
     {:event-channel (om/get-shared owner [:channels :event-channel])})
   om/IDisplayName
   (display-name [_]
     "clip-view")
   om/IRender
   (render [this]
     (dom/tr #js {:className "clip" :ref "clip-row" }
             (dom/td nil
                     (dom/a #js {:href (:url clip) :target "new"} (:title clip)))
             (dom/td #js {:className "delete"}
                     (dom/a #js {:className "btn btn-danger"
                                 :ref "delete-clip"
                                 :onClick #(put! (om/get-state owner :event-channel) [:destroy @clip])
                                 } "delete"))))))

(defn clips-view [{:keys [clips]} owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "clips-view")
    om/IRender
    (render [this]
      (dom/div #js {:className "well"}
               (dom/table #js {:className "table table-bordered table-hover table-striped"}
                (if (empty? clips)
                 (dom/tbody nil
                            (dom/tr nil
                                    (dom/td #js {:className "text-center" :colSpan "2"} "No Clips!" )))
                 (apply dom/tbody nil (om/build-all clip-view clips {:key :id}))))))))

(defn capture-panel [{clips :clips {:keys [title url] :as current-tab} :current-tab} owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "capture-panel")
    om/IInitState
    (init-state [_]
      {:title "loading..."
       :url "loading...."
       :event-channel (om/get-shared owner [:channels :event-channel])})
    om/IWillMount
      (will-mount [_]
        (let [ch (get-active-tab)]
          (go-loop []
            (let [{:keys [tab]} (<! ch)]
              (om/update! current-tab tab)
              (recur)))))
    om/IRender
    (render [this]
      (dom/div #js {:className "clip-form"}
               (dom/legend nil "Capture Url")
               (om/build ui/text-box current-tab {:opts {:label "title" :k :title :needs-focus true}})
               (om/build ui/text-box current-tab {:opts {:label "url" :k :url :needs-focus false}})
               (dom/div #js {:className "control-group"}
                    (dom/div #js {:className "controls"}
                             (dom/a #js {:className "btn btn-primary"
                                         :ref "copy-clip"
                                         :href "#"
                                         :onClick (fn [e]
                                                    (let [input (q "input[name=url]")]
                                                      (set! (.-value input) (:url @current-tab))
                                                      (.select input)
                                                      (.execCommand js/document "copy")))}
                                    "Copy")
                             (dom/a #js {:className "btn btn-success"
                                         :ref "new-clip"
                                         :href "#"
                                         :onClick (fn [e]
                                                    (let [comm (om/get-state owner :event-channel)
                                                          new-clip {:id (guid) :title (:title @current-tab) :url (:url @current-tab)}]
                                                      (put! comm [:save new-clip])))
                                         } "Capture" )))))))

(defn ^:export root [data owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "root")
    om/IWillMount
    (will-mount [this]
      (let [comm (om/get-shared owner [:channels :event-channel])]
        (om/set-state! owner :comm comm)
          (go-loop []
            (let [[type clip] (<! comm)]
              (handle-event type data clip))
              (recur))))
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