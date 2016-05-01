(require '[cljs.build.api :as b])

(b/watch "src"
  {:main 'vr-daw.core
   :output-to "out/vr_daw.js"
   :output-dir "out"})
