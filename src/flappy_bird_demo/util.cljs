(ns flappybird.util
  (:require
   [flappybird.log :as log]))

(defn floor [x] (.floor js/Math x))

(defn translate [start-pos vel time]
  (floor (+ start-pos (* time vel))))

(defn px [n] (str n "px"))

