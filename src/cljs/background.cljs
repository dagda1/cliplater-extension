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
          (log/debug (str "tab updated: " (:url tab)))
          )
        (recur)
        )
      )
    )


  (let [ch (tabs/tab-replaced-events)]
		(go-loop []
			(when-let [{:keys [added removed]} (<! ch)]
				(let [ch (tabs/get-tab added)]
					(async/take! ch
						(fn [{:keys [tab]} ch]
							(log/debug (str "replaced " (:url tab))))))
				(recur))))
  )