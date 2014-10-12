(ns background
  (:require
   [cljs.core.async :as async]
   [khroma.log :as log]
   [khroma.tabs :as tabs]
   )
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]
   )
  )

(defn init []
  (log/debug "backrgound init")

  (let [ch (tabs/tab-updated-events)]
    (go-loop []
      (when-let [{:keys [tabId changeInfo tab]} (<! ch)]
        (when (= "complete" (:status changeInfo))
          (js/alert (str "tab updated: " (:url tab)))
          )
        (recur)
        )
      )
    )

  (let [ch (tabs/tab-replaced-events)]
    (go-loop []
      (when-let [{:keys [added removed]} (<! ch)]
        (js/alert (str "tab replaced " added " to " removed))

        (let [ch (tabs/get-tab added)]
          (async/take! ch
                       (fn [{:keys [tab]} ch]
                         (js/alert (:url tab))
                         )
                       )
          )
        )
      )
    )
  )