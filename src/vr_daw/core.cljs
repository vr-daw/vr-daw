(ns vr-daw.core
  (:require [spacetime.camera :refer [create-perspective-camera init-camera!]]
            [spacetime.controls.original :as controls]
            [spacetime.core :as spacetime]
            [reagent.core :as r]
            [reagent.interop :refer-macros [$]]
            [vr-daw.components :refer [PauseComponent]]
            [cljsjs.three]
            [weasel.repl :as repl]
            [mtl-loader]
            [obj-loader]))

(def state (r/atom {:request-id nil
                    :velocity {:x 0 :y 0 :z 0}
                    :momentum {:x 0 :y 0 :z 0}
                    :can-jump false
                    :paused? true}))
(def height 10)

(def request-id (r/cursor state [:request-id]))

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

(defn diamond-color
  []
  (let [color (js/THREE.Color.)]
    (.setHSL color
             (+ (* (js/Math.random) 0.3) 0.5)
             0.75
             (+ (* (js/Math.random) 0.25) 0.75))
    color))

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

(defn diamond-box
  []
  (let [geometry (js/THREE.BoxGeometry. 20 20 20)
        _ (doall
           (map #(do (aset (.-vertexColors %) 0
                           (diamond-color))
                     (aset (.-vertexColors %) 1
                           (diamond-color))
                     (aset (.-vertexColors %) 2
                           (diamond-color)))
                (.-faces geometry)))
        material (js/THREE.MeshPhongMaterial. (clj->js
                                               {:specular 0xffffff
                                                :shading THREE.FlatShading
                                                :vertexColors THREE.VertexColors}))
        mesh (js/THREE.Mesh. geometry material)]
    (.color.setHSL material
                   (+ (* (js/Math.random) 0.3) 0.5)
                   0.75
                   (+ (* (js/Math.random) 0.25) 0.75))
    (.position.set mesh 0 10 0)
    (ThreeObject. geometry material mesh)))

;; because of the error
;;Caused by: clojure.lang.ExceptionInfo: No such namespace: three, could not locate three.cljs, three.cljc, or Closure namespace "three" in file out/spacetime/camera.cljs
;; we need to be properly packaging the foreign dependecies
;; https://github.com/clojure/clojurescript/wiki/Packaging-Foreign-Dependencies
;; however, in the future a cljsjs should be made for THREE and the supporting libs we use for it
(def scene (spacetime/create-scene))

(aset scene "fog" (THREE.Fog. 0xffffff 1 750))


(def camera (init-camera! (create-perspective-camera
                           75
                           (/ (.-innerWidth js/window)
                              (.-innerHeight js/window))
                           1
                           1000)
                          scene
                          [0 0 0]))

(def pointer-lock-controls (spacetime/pointer-lock-controls camera))

;; volumetric vectors of collision
(def collision-front (THREE.Raycaster. (THREE.Vector3.)
                                       (THREE.Vector3. 0 0 -1)
                                       0
                                       2))

(def collision-back (THREE.Raycaster. (THREE.Vector3.)
                                      (THREE.Vector3. 0 0 1)
                                      0
                                      2))

(def collision-left (THREE.Raycaster. (THREE.Vector3.)
                                      (THREE.Vector3. -1 0 0) 0
                                      5))

(def collision-right (THREE.Raycaster. (THREE.Vector3.)
                                       (THREE.Vector3. 1 0 0) 0
                                       5))

(def collision-head (THREE.Raycaster. (THREE.Vector3.)
                                      (THREE.Vector3. 1 0 0) 0
                                      1))

(def raycaster (THREE.Raycaster. (THREE.Vector3.) (THREE.Vector3. 0 -1 0) 0
                                 height))



(def box1 (diamond-box))
(defn ^:export init []
  (let [renderer (spacetime/create-renderer)
        render (spacetime/render renderer scene camera)
        ;;raycaster (THREE.Raycaster. (THREE.Vector3.) (THREE.Vector3. 0 -1 0) 0 10)
        _ (.setClearColor renderer 0xffffff)
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
        _ (spacetime/pointer-lock-change-listener!
           js/document
           (fn [event]
             (let [element (.-body js/document)
                   has-pointerlock
                   (or (= js/document.pointerLockElement element)
                       (= js/document.mozPointerLockElement element)
                       (= js/document.webkitPointerLockElement element))]
               (when has-pointerlock
                 (.log js/console "pointerlock is in body")
                 (aset pointer-lock-controls "enabled" true)
                 (reset! (r/cursor state [:paused?]) false))
               (when-not has-pointerlock
                 (.log js/console "pointerlock is not in body")
                 (aset pointer-lock-controls "enabled" false)
                 (reset! (r/cursor state [:paused?]) true)))))
        skybox (let [skybox-geometry (spacetime/create-box-geometry 20000 20000 20000)
                     skybox-material (spacetime/create-mesh-basic-material
                                      (js-obj "color" 0x0000ff;;0x063140
                                              "side" js/THREE.BackSide))]
                 (js/THREE.Mesh. skybox-geometry skybox-material))
        light  (js/THREE.HemisphereLight. 0xeeeeff 0x777788 0.75)
        _ (.position.set light 0.5 1 0.75)
        ;;light (js/THREE.DirectionalLight. 0xffeedd)
        ;;light (js/THREE.AmbientLight. 0xeeeeff)
        red-sphere (sphere 200 1 1 1000)
        ;;box1 (diamond-box)
        ]
    (.add scene (.-mesh (diamond-floor nil)))
    (.add scene (.-mesh box1))
    (spacetime/translate-position! (.-mesh box1) [0 0 -20])
    ;;(.add scene skybox)
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
     (let [velocity (r/cursor state [:velocity])
           velocity-x (r/cursor velocity [:x])
           velocity-y (r/cursor velocity [:y])
           velocity-z (r/cursor velocity [:z])
           momentum (r/cursor state [:momentum])
           momentum-x (r/cursor momentum [:x])
           momentum-y (r/cursor momentum [:y])
           momentum-z (r/cursor momentum [:z])
           can-jump (r/cursor state [:can-jump])]
       (fn [delta-t]
         (let [height 10
               position-y (:y (spacetime/get-position
                               pointer-lock-controls))
               jump-max  60
               paused? (r/cursor state [:paused?])
               ]
           (render)
           (when-not @paused?
             (controls/controls-handler
              {:left-fn
               ;; #(swap! momentum-x (partial - 2))
               ;; #(swap! momentum-x #())
               ;; #(reset! momentum-x  -2)
               #(spacetime/translate-controls! pointer-lock-controls
                                               [-1 0 0])
               :up-fn
               ;; #(swap! momentum-z (partial - 2))
               ;; #(reset! momentum-z  -2)
               #(when-not (> (aget (.intersectObjects
                                    collision-front
                                    (clj->js [(.-mesh box1)]))
                                   "length")
                             0)
                  (.log js/console "face bump!")
                  (spacetime/translate-controls! pointer-lock-controls
                                                 [0 0 -1]))
               :right-fn
               ;; #(swap! momentum-x (partial + 2))
               ;; #(reset! momentum-x 2)
               #(spacetime/translate-controls! pointer-lock-controls
                                               [1 0 0])
               :down-fn
               #(spacetime/translate-controls! pointer-lock-controls
                                               [0 0 1])
               ;; #(reset! momentum-z 2)
               ;; #(swap! momentum-z (partial + 2))
               :space-fn
               ;; jump!
               #(when @can-jump
                  (swap! velocity-y (partial + 3))
                  (reset! can-jump false))})
             ;; all momentums
             ;; (swap! momentum-x #(- % (* (js/Math.abs %) (/ delta-t 1000) )))
             ;; (swap! momentum-z #(- % (* (js/Math.abs %) (/ delta-t 1000) )))
             ;; all velocities
             ;; (.log js/console "velocity-x" @velocity-x)
             ;; (.log js/console "momentum-x" @momentum-x)
             ;; (.log js/console "velocity-y" @velocity-y)
             ;; (.log js/console "velocity-z" @velocity-z)
             ;; (.log js/console "momentum-z" @momentum-z)
             ;; (.log js/console (:x (spacetime/get-position pointer-lock-controls)))
             ;; (.log js/console delta-t)
             ;; (swap! momentum-x #(- % (* (js/Math.abs %) (/ delta-t 1000) )))
             ;; (swap! momentum-z #(- % (* (js/Math.abs %) (/ delta-t 1000) )))
             ;; (+ @velocity-x (* @momentum-x (/ delta-t 1000)))
             ;; gravity's affect on velocity-y
             (swap! velocity-y (fn [velocity-y] (+ velocity-y
                                                   (* -9.8 (/ delta-t 1000)))))
             ;; movement affect on velocities x and z
             ;; (swap! velocity-x #(+ % (* @momentum-x (/ delta-t 1000))))
             ;; (swap! velocity-z #(+ % (* @momentum-z (/  delta-t 1000))))
             ;; velocity move camera
             (spacetime/translate-controls!
              pointer-lock-controls [0
                                     @velocity-y
                                     0])
             ;; gravity affect on y velocity

             ;;(.log js/console "velocity-y " @velocity-y)
             ;; movement affect in the x-y pla
             ;; floor check
             (when (<= (:y (spacetime/get-position pointer-lock-controls))
                       height)
               (spacetime/set-position!
                pointer-lock-controls
                [(:x (spacetime/get-position pointer-lock-controls))
                 height
                 (:z (spacetime/get-position pointer-lock-controls))])
               ;; hit bottom, no more gravity
               (reset! velocity-y 0)
               (reset! can-jump true))
             ;; set the raycaster to current origin of camera (or controls?)
             (.ray.origin.copy raycaster (clj->js (spacetime/get-position
                                                   pointer-lock-controls)))
             ;; for the face bump
             (.ray.origin.copy collision-front
                               (clj->js (spacetime/get-position
                                         pointer-lock-controls)))
             ;; put the raycaster origin at your feet
             (aset raycaster "ray" "origin" "y" (- (aget raycaster
                                                         "ray"
                                                         "origin"
                                                         "y")
                                                   height))
             ;; intersection check
             (when (> (aget (.intersectObjects
                             raycaster
                             (clj->js [(.-mesh box1)]))
                            "length")
                      0)
               (.log js/console "I intersected")
               (reset! velocity-y 0;(js/Math.max 0 @velocity-y)
                       )
               (reset! can-jump true))
             ;;(.log js/console (:y (spacetime/get-position pointer-lock-controls)))

             ;;(.log js/console @velocity-x)
             ;;(.log js/console "velocity-y " @velocity-y)
             ))))
     (r/cursor state [:request-id]))
    ;; add listeners for key events
    (js/addEventListener "keydown" controls/game-key-down! true)
    (js/addEventListener "keyup"   controls/game-key-up!   true)
    ;; add Reagent
    (r/render-component
     [PauseComponent
      {:paused? (r/cursor state [:paused?])
       :on-click (fn [event]
                   (-> (.-body js/document)
                       (.requestPointerLock))
                   (reset! (r/cursor state [:paused?]) false)
                   (aset pointer-lock-controls "enabled" true))}]
     (.getElementById js/document "reagent-app"))))

(when-not (repl/alive?)
  (repl/connect "ws://127.0.0.1:9001"))
