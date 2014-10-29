(ns cliplater.util)

; lots of methods here
; https://github.com/frenchy64/typed-clojurescript-play/blob/master/src/cljs/cljs_play/dom.cljs#L8

(defn q [selector]
  (.querySelector js/document selector))