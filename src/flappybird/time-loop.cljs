(ns flappybird.time-loop
  (:require
   [flappybird.defs :refer [bottom-y horiz-vel gravity jump-vel start-y
                            flappy-x flappy-width flappy-height]]
   [flappybird.log :as log]

   [flappybird.pillars :refer [in-pillar? in-pillar-gap? pillar-counter pillar-fn
                               pillar-offsets pillar-spacing update-pillars]]

   [flappybird.util :refer [floor px translate]]))

;; time-loop     -> time-update -> assoc timestamp
;;                              -> update-flappy
;;                              -> update-pillars
;;                              -> collision?
;;                              -> score
;;
;;               -> requestAnimationFrame -> time-loop
;;

(declare collision?)
(declare score)
(declare update-flappy)

(defn time-update [timestamp world-state]
  (-> world-state
      (assoc
       :cur-time timestamp
       :time-delta (- timestamp (:flappy-start-time world-state)))
      update-flappy
      update-pillars
      collision?
      score))

(defn time-loop [time world-reference]
  ;; "partial: Takes a function f and fewer than the normal arguments
  ;; to f, and returns a fn that takes a variable number of additional
  ;; args. When called, the returned function calls f with args +
  ;; additional args."
  ;; - https://clojuredocs.org/clojure.core/partial
  (let [new-state (swap! world-reference (partial time-update time))]
    (when (:timer-running new-state)
      ;; https://developer.mozilla.org/en-US/docs/Web/API/window/requestAnimationFrame
      (.requestAnimationFrame js/window (fn [time] (time-loop time world-reference))))))

(defn score [{:keys [cur-time start-time] :as world-state}]
  (let [score (- (.abs js/Math (floor (/ (- (* (- cur-time start-time) horiz-vel) 544)
                               pillar-spacing)))
                 4)]
    (assoc world-state :score (if (neg? score) 0 score))))

(defn bottom-collision? [{:keys [flappy-y]}]
  (>= flappy-y (- bottom-y flappy-height)))

(defn collision? [{:keys [pillar-list] :as st}]
  (if (some #(or (and (in-pillar? %)
                      (not (in-pillar-gap? st %)))
                 (bottom-collision? st)) pillar-list)
    (assoc st :timer-running false)
    st))

(defn sine-wave [world-state]
  (assoc world-state
    :flappy-y
    (+ start-y (* 30 (.sin js/Math (/ (:time-delta world-state) 300))))))

(defn update-flappy [{:keys [time-delta initial-vel flappy-y jump-count] :as world-state}]
  (log/debug (str "update-flappy:  jumps so far: " jump-count))
  (if (pos? jump-count)
    (let [cur-vel (- initial-vel (* time-delta gravity))
          new-y   (- flappy-y cur-vel)
          new-y   (if (> new-y (- bottom-y flappy-height))
                    (- bottom-y flappy-height)
                    new-y)]
      (assoc world-state
        :flappy-y new-y))
    (sine-wave world-state)))

