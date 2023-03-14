(ns flappy-bird-demo.core
  (:require
   [sablono.core :as sab :include-macros true]
   [flappybird.defs :refer [bottom-y horiz-vel gravity jump-vel start-y
                            flappy-x flappy-width flappy-height]]
   [flappybird.log :as log]
   [flappybird.pillars :refer [in-pillar? in-pillar-gap? pillar-counter pillar-fn
                               pillar-offsets pillar-spacing update-pillars]]
   [flappybird.util :refer [floor px translate]]))

(def starting-state {:timer-running false
                     :jump-count 0
                     :initial-vel 0
                     :start-time 0
                     :flappy-start-time 0
                     :flappy-y start-y
                     :pillar-list
                     [{:start-time 0
                       :pos-x 900
                       :cur-x 900
                       :gap-top 200}]})

(defonce world-reference (atom starting-state))

(defn bottom-collision? [{:keys [flappy-y]}]
  (>= flappy-y (- bottom-y flappy-height)))

(defn collision? [{:keys [pillar-list] :as st}]
  (if (some #(or (and (in-pillar? %)
                      (not (in-pillar-gap? st %)))
                 (bottom-collision? st)) pillar-list)
    (assoc st :timer-running false)
    st))

(defn sine-wave [st]
  (assoc st
    :flappy-y
    (+ start-y (* 30 (.sin js/Math (/ (:time-delta st) 300))))))

(defn update-flappy [{:keys [time-delta initial-vel flappy-y jump-count] :as world-state}]
  (log/debug (str "update-flappy:  world-state keys: " (keys world-state)))
  (if (pos? jump-count)
    (let [cur-vel (- initial-vel (* time-delta gravity))
          new-y   (- flappy-y cur-vel)
          new-y   (if (> new-y (- bottom-y flappy-height))
                    (- bottom-y flappy-height)
                    new-y)]
      (assoc world-state
        :flappy-y new-y))
    (sine-wave world-state)))

(defn score [{:keys [cur-time start-time] :as st}]
  (let [score (- (.abs js/Math (floor (/ (- (* (- cur-time start-time) horiz-vel) 544)
                               pillar-spacing)))
                 4)]
  (assoc st :score (if (neg? score) 0 score))))

(defn time-update [timestamp state]
  (-> state
      (assoc
       :cur-time timestamp
       :time-delta (- timestamp (:flappy-start-time state)))
      update-flappy
      update-pillars
      collision?
      score))

(defn jump [{:keys [cur-time jump-count] :as state}]
  (-> state
      (assoc
       :jump-count (inc jump-count)
       :flappy-start-time cur-time
       :initial-vel jump-vel)))

;; derivatives

(defn border [{:keys [cur-time] :as state}]
  (-> state
      (assoc :border-pos (mod (translate 0 horiz-vel cur-time) 23))))

(defn world [state]
  (-> state
      border
      pillar-offsets))

(defn next-pillar-key []
  (log/debug (str "time for another pillar! current-counter: " @pillar-counter))
  (do (swap! pillar-counter inc) @pillar-counter))

(defn time-loop [time]
  (let [new-state (swap! world-reference (partial time-update time))]
    (when (:timer-running new-state)
      ;; https://developer.mozilla.org/en-US/docs/Web/API/window/requestAnimationFrame
      (.requestAnimationFrame js/window time-loop))))

(defn reset-state [_ cur-time]
  (-> starting-state
      (update :pillar-list (partial map #(assoc % :start-time cur-time)))
      (assoc
       :start-time cur-time
       :flappy-start-time cur-time
       :timer-running true)))

(defn start-game []
  (.requestAnimationFrame
   js/window
   (fn [time]
     (reset! world-reference (reset-state @world-reference time))
     (time-loop time))))

(defn main-template [{:keys [score cur-time jump-count
                             timer-running border-pos
                             flappy-y pillar-list] :as world}]
  (reset! pillar-counter 0)
  ;; https://github.com/r0man/sablono
  (sab/html [:div.board {:onMouseDown (fn [e]
                                        (swap! world-reference jump)
                                        (.preventDefault e))}
             [:h1.score score]
             (if-not timer-running
               (sab/html [:a.start-button {:onClick #(start-game)}
                (if (< 1 jump-count) "Herstart" "Start")])
               (sab/html [:span]))
             [:div (map pillar-fn pillar-list)]
             [:div.flappy {:style {:top (px flappy-y)}}]
             [:div.scrolling-border {:style {:background-position-x (px border-pos)}}]]))

(defn renderer [world-state]
  ;; https://beta.reactjs.org/reference/react-dom/render
  (.render js/ReactDOM (main-template world-state)
           ;; see index.html for <div id='board-area'>:
           (.getElementById js/document "board-area")))

;; https://clojuredocs.org/clojure.core/add-watch
;; "Adds a watch function to an agent/atom/var/ref reference...
;;  Whenever the reference's state might have been changed,
;;  any registered watches will have their functions called...
;;  Keys [in our case, ':renderer-of-the-world' is such a key] must be unique
;;  per reference, and can be used to remove the watch with remove-watch,
;;  but are otherwise considered opaque by the watch mechanism."
(add-watch world-reference :renderer-of-the-world
           (fn [key reference old-world-state new-world-state]
             ;; note that all of these provided arguments
             ;; are ignored *except* new-state:
             (log/debug (str "world-reference has changed to: " new-world-state "; time to call (renderer)!"))
             (renderer (world new-world-state))))

(defn get-this-party-started []
  (log/info (str "let's get this party started!"))
  ;; this causes the above ':renderer-of-the-world' watch to fire:
  (reset! world-reference @world-reference))

(get-this-party-started)
