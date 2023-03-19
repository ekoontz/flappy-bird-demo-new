(ns flappy-bird-demo.core
  (:require
   [sablono.core :as sab :include-macros true]
   [flappybird.actions :refer [border jump]]
   [flappybird.defs :refer [bottom-y horiz-vel gravity jump-vel start-y
                            flappy-x flappy-width flappy-height]]
   [flappybird.pillars :refer [in-pillar? in-pillar-gap? pillar-counter pillar-fn
                               pillar-offsets pillar-spacing update-pillars]]
   [flappybird.log :as log]
   [flappybird.time-loop :refer [collision? score sine-wave
                                 update-flappy time-loop time-update]]
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

;; add-watch -> world -> renderer -> main-template
;;
;; main-template -> (mousedown) -> jump
;;               -> (click start button) -> start-game -> time-loop
;;
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
(declare renderer)
(declare start-game)
(declare world)
(add-watch world-reference :renderer-of-the-world
           (fn [key reference old-world-state new-world-state]
             ;; Note that key, reference and old-world-state are all
             ;; ignored (only new-world-state is used below):
             (log/debug (str "world-reference has changed to: " new-world-state "; time to call (renderer)!"))
             (renderer (world new-world-state))))

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

(defn world [world-state]
  (-> world-state
      border
      pillar-offsets))

(defn reset-state [_ cur-time]
  (-> starting-state
      (update :pillar-list (partial map #(assoc % :start-time cur-time)))
      (assoc
       :start-time cur-time
       :flappy-start-time cur-time
       :timer-running true)))

(defn start-game []
  ;; https://developer.mozilla.org/en-US/docs/Web/API/window/requestAnimationFrame
  (.requestAnimationFrame
   js/window
   (fn [time]
     (reset! world-reference (reset-state @world-reference time))
     (time-loop time world-reference))))

(defn renderer [world-state]
  ;; https://beta.reactjs.org/reference/react-dom/render
  (.render js/ReactDOM (main-template world-state)
           ;; see index.html for <div id='board-area'>:
           (.getElementById js/document "board-area")))

;; this causes the above ':renderer-of-the-world' watch to fire:
(reset! world-reference @world-reference)


