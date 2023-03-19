(ns flappybird.actions
  (:require
   [flappybird.defs :refer [bottom-y horiz-vel gravity jump-vel start-y
                            flappy-x flappy-width flappy-height]]
   [flappybird.log :as log]
   [flappybird.util :refer [floor px translate]]))

(defn jump [{:keys [cur-time jump-count] :as world-state}]
  (-> world-state
      (assoc
       :jump-count (inc jump-count)
       :flappy-start-time cur-time
       :initial-vel jump-vel)))

(defn border [{:keys [cur-time] :as world-state}]
  (-> world-state
      (assoc :border-pos (mod (translate 0 horiz-vel cur-time) 23))))

(defn reset-game [starting-state current-time]
  (-> starting-state
      (update :pillar-list (partial map #(assoc % :start-time current-time)))
      (assoc
       :start-time current-time
       :flappy-start-time current-time
       :timer-running true)))

(defn start-game [starting-state world-reference time-loop-fn]
  ;; https://developer.mozilla.org/en-US/docs/Web/API/window/requestAnimationFrame
  (.requestAnimationFrame
   js/window
   (fn [current-time]
     (reset! world-reference (reset-game starting-state current-time))
     (time-loop-fn current-time world-reference))))
