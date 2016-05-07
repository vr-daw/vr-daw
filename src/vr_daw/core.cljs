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
                              (set! (.-position.z object) 1279)
                              (.add scene object)
                              )))))]
    (doto mtlLoader
      (.setBaseUrl  "assets/")
      (.setPath "assets/")
      (.load "pianorollshape.mtl" onLoad))))

(defn diamond-floor
  [scene]
  (let [geometry (js/THREE.PlaneGeometry. 2000 2000 100 100)
        _ (.rotateX geometry (- (/ js/Math.PI 2)))]
    geometry))

;; because of the error
;;Caused by: clojure.lang.ExceptionInfo: No such namespace: three, could not locate three.cljs, three.cljc, or Closure namespace "three" in file out/spacetime/camera.cljs
;; we need to be properly packaging the foreign dependecies
;; https://github.com/clojure/clojurescript/wiki/Packaging-Foreign-Dependencies
;; however, in the future a cljsjs should be made for THREE and the supporting libs we use for it

(defn ^:export init []
  (let [scene (spacetime/create-scene)
        camera (init-camera! (create-perspective-camera
                              45
                              (/ (.-innerWidth js/window)
                                 (.-innerHeight js/window))
                              0.1
                              20000)
                             scene
                             [0 0 1300])
        renderer (spacetime/create-renderer)
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
        pointer-lock-controls (spacetime/pointer-lock-controls camera)
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
    (spacetime/start-time-frame-loop (fn [delta-t]
                                       (do (render)
                                           (controls/controls-handler camera)))
                                     request-id)
    ;; add listeners for key events
    (js/addEventListener "keydown" controls/game-key-down! true)
    (js/addEventListener "keyup"   controls/game-key-up!   true)))

(when-not (repl/alive?)
  (repl/connect "ws://127.0.0.1:9001"))
