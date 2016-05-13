(ns vr-daw.components
  (:require [reagent.core :as r]))

(defn PauseComponent
  "Props is:
  {:paused? ; r/atom boolean
  :on-click ; fn
  }
  "
  [props]
  (fn [props]
    (let [{:keys [paused?
                  on-click]} props
          merge-block (if @paused?
                        {}
                        {:display "none"})]
      [:div {:id "blocker"
             :style (merge merge-block
                           {:position "absolute"
                            :width "100%"
                            :height "100%"
                            :background-color "rgba(0,0,0,0.5)"})}
       [:div {:id "instructions"
              :style (merge merge-block
                            {:width "100%"
                             :height "100%"
                             :display "box"
                             :box-orient "horizontal"
                             :box-align "center"
                             :color "#ffffff"
                             :text-align "center"
                             :cursor "pointer"})
              :on-click on-click}
        [:span {:style {:font-size "40px"}}
         "Click to play"]
        [:br]
        "(W, A, S, D = Move, SPACE = Jump, MOUSE = Look around)"]])))
