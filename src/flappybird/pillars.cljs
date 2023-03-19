(ns flappybird.pillars
  (:require
   [flappybird.defs :refer [bottom-y horiz-vel gravity jump-vel start-y flappy-x flappy-width flappy-height]]
   [flappybird.util :refer [px translate]]
   [flappybird.log :as log]
   [sablono.core :as sab :include-macros true]))

(defonce number-of-pillars 3)
(def pillar-spacing 324)
(def pillar-gap 158)
(def pillar-width 46)
(def pillar-counter (atom 0))

(defn curr-pillar-pos [cur-time {:keys [pos-x start-time]}]
  (translate pos-x horiz-vel (- cur-time start-time)))

(defn in-pillar? [{:keys [cur-x]}]
  (and (>= (+ flappy-x flappy-width) cur-x)
       (< flappy-x (+ cur-x pillar-width))))

(defn in-pillar-gap? [{:keys [flappy-y]} {:keys [gap-top]}]
  (and (< gap-top flappy-y)
       (> (+ gap-top pillar-gap)
          (+ flappy-y flappy-height))))

(defn new-pillar [cur-time pos-x]
  (let [gap-top (+ 60 (rand-int (- bottom-y 120 pillar-gap)))]
    {:start-time cur-time
     :pos-x      pos-x
     :cur-x      pos-x
     :gap-top    gap-top}))

(defn update-pillars [{:keys [pillar-list cur-time] :as world-state}]
  (log/debug (str "update-pillars: world-state keys: " (keys world-state)))
  (let [pillars-with-pos (map #(assoc % :cur-x (curr-pillar-pos cur-time %)) pillar-list)

        ;; https://clojuredocs.org/clojure.core/sort-by
        pillars-in-world (sort-by
                          :cur-x
                          (filter #(> (:cur-x %) (- pillar-width)) pillars-with-pos))]
    (log/debug (str "pillars with-pos: " pillars-with-pos))
    (assoc world-state
      :pillar-list
      (if (< (count pillars-in-world) number-of-pillars)
        (let [new-pillar (new-pillar
                          cur-time
                          (+ pillar-spacing
                             (:cur-x (last pillars-in-world))))]
          (log/info (str "ADDING A NEW PILLAR: " new-pillar))
          (conj pillars-in-world new-pillar))
        pillars-in-world))))

(defn pillar-offset [{:keys [gap-top] :as p}]
  (assoc p
         :upper-height gap-top
         :lower-height (- bottom-y gap-top pillar-gap)))

(defn pillar-offsets [state]
  (update-in state [:pillar-list]
             (fn [pillar-list]
               (map pillar-offset pillar-list))))

(defn next-pillar-key []
  (log/debug (str "time for another pillar! current-counter: " @pillar-counter))
  (do (swap! pillar-counter inc) @pillar-counter))

(defn pillar-fn [{:keys [cur-x pos-x upper-height lower-height]}]
  (let [pillar-key (next-pillar-key)]
    ;; https://github.com/r0man/sablono
    (sab/html
     [:div.pillars {:key pillar-key}
      [:div.pillar.pillar-upper {:style {:left (px cur-x)
                                         :height upper-height}}]
      [:div.pillar.pillar-lower {:style {:left (px cur-x)
                                         :height lower-height}}]])))
