(ns flappybird.util
  (:require
   [flappybird.log :as log]))

(defn floor [x] (.floor js/Math x))

(defn translate [start-pos vel time]
  (floor (+ start-pos (* time vel))))

(defn px [n] (str n "px"))

(defn indent [this-many]
  (clojure.string/join (take this-many (repeatedly (fn [] " ")))))

(defn format-world-state [world-state indent-this-many]
  (let [indentation (indent indent-this-many)
        indentation-plus-1 (indent (+ 1 indent-this-many))]
    (cond
      (or (seq? world-state)
          (vector? world-state))
      (str "[\n"
           (clojure.string/join ",\n" (map (fn [x] (format-world-state
                                                    x
                                                    (+ 1 indent-this-many)))
                                           world-state))
           "]")
      (map? world-state)
      (str
       indentation "{\n"
       (clojure.string/join "\n"
                            (map (fn [k]
                                   (str indentation-plus-1 k " "
                                        (format-world-state (get world-state k) (+ 1 indent-this-many))))
                                 (keys world-state)))
       "\n" indentation "}")
      :else (str world-state))))
