(defproject cliplater "0.1.0-SNAPSHOT"
  :description ""
  :url "http://example.com/FIXME"

  :dependencies [
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2311"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [sablono "0.2.22"]
                 [om "0.7.3"]
                 [cljs-http "0.1.17-SNAPSHOT"]
                 ]

  :plugins [
            [lein-cljsbuild "1.0.4-SNAPSHOT"]
            ]

  :hooks  [leiningen.cljsbuild]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src/cljs"]
              :compiler {
                :output-dir "extension/js/compiled"
                :output-to "extension/js/compiled/cliplater.js"
                :source-map "extension/js/compiled/cliplater.js.map"
                :optimizations :none
                :pretty-print true
                :source-map true}}]})