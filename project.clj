(defproject vr-daw "0.1.0-SNAPSHOT"
  :description "A 3D Digital Audio Workspace"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.51"]
                 [weasel "0.7.0" :exclusions [org.clojure/clojurescript]]
                 [spacetime "0.2.0-SNAPSHOT"]
                 [reagent "0.6.0-alpha2"]]
  ;; :jvm-opts ^:replace ["-Xmx1g" "-server"]
  :plugins [[lein-npm "0.6.1"]
            [lein-cljsbuild "1.1.2"]
            [lein-externs "0.1.5"]]
  :npm {:dependencies [[source-map-support "0.4.0"]]}
  :source-paths ["src" "target/classes"]
  :clean-targets ["out" "release"]
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src"]
                        :compiler {
                                   :main vr-daw.core
                                   :output-to "out/vr_daw.js"
                                   :output-dir "out"
                                   :optimizations :none
                                   :pretty-print true
                                   :source-map true}}
                       {:id "release"
                        :source-paths ["src"]
                        :compiler {
                                   :main vr-daw.core
                                   :output-to "release/vr_daw.min.js"
                                   :optimizations :advanced
                                   :pretty-print false
                                   :externs ["js/vr_daw_externs.js"]}}]}
  :target-path "target")
