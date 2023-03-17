(ns flappybird.log)

(defmacro info [args]
  `(apply js/console.info (to-array ~[args])))

(defmacro debug [args]
  `(apply js/console.debug (to-array ~[args])))

(defmacro error [args]
  `(apply js/console.error (to-array ~[args])))

(defmacro warn [args]
  `(apply js/console.warn (to-array ~[args])))





