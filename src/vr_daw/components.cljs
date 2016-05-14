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
                  on-click]} props]
      [:div {:id "blocker"
             :style (if @paused?
                      {}
                      {:display "none"})}
       [:div {:id "instructions"
              :style (if @paused?
                       {}
                       {:display "none"})
              :on-click on-click}
        [:span {:style {:font-size "40px"}}
         "Click to play"]
        [:br]
        "(W, A, S, D = Move, SPACE = Jump, MOUSE = Look around)"]])))
