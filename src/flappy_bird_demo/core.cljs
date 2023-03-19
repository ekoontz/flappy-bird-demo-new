(ns flappy-bird-demo.core
  (:require
   [sablono.core :as sab :include-macros true]
   [flappybird.defs :refer [bottom-y horiz-vel gravity jump-vel start-y
                            flappy-x flappy-width flappy-height]]
   [flappybird.pillars :refer [in-pillar? in-pillar-gap? pillar-counter pillar-fn
                               pillar-offsets pillar-spacing update-pillars]]
   [flappybird.util :refer [floor px translate]]
   [flappybird.log :as log]))

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

(defn score [{:keys [cur-time start-time] :as world-state}]
  (let [score (- (.abs js/Math (floor (/ (- (* (- cur-time start-time) horiz-vel) 544)
                               pillar-spacing)))
                 4)]
    (assoc world-state :score (if (neg? score) 0 score))))

(defn jump [{:keys [cur-time jump-count] :as world-state}]
  (-> world-state
      (assoc
       :jump-count (inc jump-count)
       :flappy-start-time cur-time
       :initial-vel jump-vel)))

(defn border [{:keys [cur-time] :as world-state}]
  (-> world-state
      (assoc :border-pos (mod (translate 0 horiz-vel cur-time) 23))))

(defn world [world-state]
  (-> world-state
      border
      pillar-offsets))

(defn time-update [timestamp world-state]
  (-> world-state
      (assoc
       :cur-time timestamp
       :time-delta (- timestamp (:flappy-start-time world-state)))
      update-flappy
      update-pillars
      collision?
      score))

(defn time-loop [time]

  ;; "partial: Takes a function f and fewer than the normal arguments
  ;; to f, and returns a fn that takes a variable number of additional
  ;; args. When called, the returned function calls f with args +
  ;; additional args."
  ;; - https://clojuredocs.org/clojure.core/partial
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

(declare main-template)

(defn start-game []
  ;; https://developer.mozilla.org/en-US/docs/Web/API/window/requestAnimationFrame
  (.requestAnimationFrame
   js/window
   (fn [time]
     (reset! world-reference (reset-state @world-reference time))
     (time-loop time))))

(defn renderer [world-state]
  ;; https://beta.reactjs.org/reference/react-dom/render
  (.render js/ReactDOM (main-template world-state)
           ;; see index.html for <div id='board-area'>:
           (.getElementById js/document "board-area")))

;; add-watch -> renderer -> main-template
;;
;; main-template -> (mousedown) -> jump
;;               -> (click start button) -> start-game

;; time-loop     -> time-update -> assoc timestamp
;;                              -> update-flappy
;;                              -> update-pillars
;;                              -> collision?
;;                              -> score
;;
;;               -> requestAnimationFrame -> reset-state
;;                                        -> time-loop
;;
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

;; https://clojuredocs.org/clojure.core/add-watch
;; "Adds a watch function to an agent/atom/var/ref reference...
;;  Whenever the reference's state might have been changed,
;;  any registered watches will have their functions called...
;;  Keys must be unique  per reference, and can be used to remove
;;  the watch with remove-watch, but are otherwise considered
;;  opaque by the watch mechanism."
;; 
;; In our case, ':renderer-of-the-world' is such a key, but we don't yet
;; use it for anything (e.g. by calling remove-watch). 
(add-watch world-reference :renderer-of-the-world
           (fn [key reference old-world-state new-world-state]
             ;; Note that key, reference and old-world-state are all
             ;; ignored (only new-world-state is used below):
             (log/debug (str "world-reference has changed to: " new-world-state "; time to call (renderer)!"))
             (renderer (world new-world-state))))

(defn get-this-party-started []
  (log/info (str "let's get this party started!"))
  ;; this causes the above ':renderer-of-the-world' watch to fire:
  (reset! world-reference @world-reference))

(get-this-party-started)
