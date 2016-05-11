(ns vr-daw.core
  (:require [spacetime.camera :refer [create-perspective-camera init-camera!]]
            [spacetime.controls.original :as controls]
            [spacetime.core :as spacetime]
            [cljsjs.three]
            [weasel.repl :as repl]
            [mtl-loader]
            [obj-loader]))

(def request-id (atom nil))

;; (defonce conn
;;   (repl/connect "http://localhost:9000/repl"))
(deftype Sphere
    [geometry material mesh selected?]
  Object
  ;; specify the new color using hexadecial notation
  (change-color [this color]
    (.material.color.set this color)))
(defn sphere

  "Create a sphere with initial coordinates x,y,z and radius."
  [x y z radius]
  (let [geometry (js/THREE.SphereGeometry. radius 32 16)
        material (js/THREE.MeshBasicMaterial. (js-obj
                                               "color" 0xFF0000
                                               "side" js/THREE.DoubleSide))
        mesh     (js/THREE.Mesh. geometry material)
        selected? false]
    (.position.set mesh x y z)
    (Sphere. geometry material mesh selected?)))

(defn PianoLoader
  [scene]
  (let [mtlLoader (js/THREE.MTLLoader.)
        onLoad (fn [materials]
                 (let [_ (.preload materials)
                       objLoader (js/THREE.OBJLoader.)]
                   (doto objLoader
                     (.setMaterials materials)
                     (.setPath "assets/")
                     (.load "pianorollshape.obj"
                            (fn [object]
                              (set! (.-position.z object) -7)
                              (set! (.-position.y object) 7)
                              ;;(set! (.-position.y object) 10)
                              ;;(.position.set object [0 0 100])
                              (.add scene object)
                              )))))]
    (doto mtlLoader
      (.setBaseUrl  "assets/")
      (.setPath "assets/")
      (.load "pianorollshape.mtl" onLoad))))

(deftype ThreeObject
    [geometry material mesh])

(defn diamond-floor
  [scene]
  (let [geometry (js/THREE.PlaneGeometry. 2000 2000 100 100)
        _ (.rotateX geometry (- (/ js/Math.PI 2)))
        _ (doall
           (map #(do (aset % "x" (+ (- (* (js/Math.random) 20) 10)
                                    (aget % "x")))
                     (aset % "y" (+ (* (js/Math.random) 2)
                                    (aget % "y")))
                     (aset % "z" (+ (- (* (js/Math.random) 20) 10)
                                    (aget % "z"))))
                (.-vertices geometry)))
        color (fn [] (let [color (js/THREE.Color.)]
                       (.setHSL color
                                (+ (* (js/Math.random) 0.3) 0.5)
                                0.75
                                (+ (* (js/Math.random) 0.25) 0.75))
                       color))
        _ (doall
           (map #(do (aset (.-vertexColors %) 0
                           (color))
                     (aset (.-vertexColors %) 1
                           (color))
                     (aset (.-vertexColors %) 2
                           (color)))
                (.-faces geometry)))
        material (js/THREE.MeshBasicMaterial.
                  (clj->js
                   {:vertexColors js/THREE.VertexColors}))
        mesh (js/THREE.Mesh. geometry material)]
    (ThreeObject. geometry material mesh)))

;; because of the error
;;Caused by: clojure.lang.ExceptionInfo: No such namespace: three, could not locate three.cljs, three.cljc, or Closure namespace "three" in file out/spacetime/camera.cljs
;; we need to be properly packaging the foreign dependecies
;; https://github.com/clojure/clojurescript/wiki/Packaging-Foreign-Dependencies
;; however, in the future a cljsjs should be made for THREE and the supporting libs we use for it
(def scene (spacetime/create-scene))

(aset scene "fog" (THREE.Fog. 0xffffff 0 750))

(def camera (init-camera! (create-perspective-camera
                           75
                           (/ (.-innerWidth js/window)
                              (.-innerHeight js/window))
                           1
                           1000)
                          scene
                          [0 0 0]))
(def pointer-lock-controls (spacetime/pointer-lock-controls camera))

(defn ^:export init []
  (let [renderer (spacetime/create-renderer)
        render (spacetime/render renderer scene camera)
        container (-> js/document
                      (.getElementById "ThreeJS"))
        _ (spacetime/attach-renderer! renderer container)
        _ (.addEventListener container "click"
                             (fn [event]
                               (-> js/document
                                   .-body
                                   ;; we should stub this out for webkit and moz
                                   (.requestPointerLock))))
        ;;pointer-lock-controls (spacetime/pointer-lock-controls camera)
        _ (.add scene (.getObject pointer-lock-controls))
        _ (spacetime/pointer-lock-listener! js/document pointer-lock-controls)
        skybox (let [skybox-geometry (spacetime/create-box-geometry 20000 20000 20000)
                     skybox-material (spacetime/create-mesh-basic-material
                                      (js-obj "color" 0x063140
                                              "side" js/THREE.BackSide))]
                 (js/THREE.Mesh. skybox-geometry skybox-material))
        ;;light  (js/THREE.HemisphereLight. 0xeeeeff 1.0 )
        light (js/THREE.DirectionalLight. 0xffeedd)
        ;;light (js/THREE.AmbientLight. 0xeeeeff)
        red-sphere (sphere 200 1 1 1000)]
    (.add scene (.-mesh (diamond-floor nil)))
    (.add scene skybox)
    (doto light
      (.position.set 0 0 1))
    (.add scene light)
    ;; add the piano roll
    (PianoLoader scene)
    ;;(.add scene (.-mesh red-sphere))
    (spacetime/window-resize! renderer camera)
    (js/THREEx.WindowResize renderer camera)
    ;;(.appendChild container (.-domElement stats))
    (.bindKey js/THREEx.FullScreen (js-obj "charCode" (.charCodeAt "m" 0)))
    (spacetime/fullscreen!)
    (spacetime/start-time-frame-loop
     (fn [delta-t]
       (let [height 10
             position-y (nth (spacetime/get-position
                              pointer-lock-controls)
                             1)
             gravity     (*  -9.8 (/ delta-t 1000) 10)
             jump-height  100
             ]
         (render)
         (controls/controls-handler
          #(spacetime/translate-controls! pointer-lock-controls
                                          [-1 0 0])
          #(spacetime/translate-controls! pointer-lock-controls
                                          [0 0 -1])
          #(spacetime/translate-controls! pointer-lock-controls
                                          [1 0 0])
          #(spacetime/translate-controls! pointer-lock-controls
                                          [0 0 1])
          #(spacetime/translate-controls! pointer-lock-controls
                                          [0
                                           (if (<= position-y
                                                   height)
                                             jump-height
                                             0)
                                           0]))
         ;; gravity
         (spacetime/translate-controls!
          pointer-lock-controls
          [0 (if (<= position-y
                     height)
               0
               gravity)
           0])
         ))
     request-id)
    ;; add listeners for key events
    (js/addEventListener "keydown" controls/game-key-down! true)
    (js/addEventListener "keyup"   controls/game-key-up!   true)))

(when-not (repl/alive?)
  (repl/connect "ws://127.0.0.1:9001"))
