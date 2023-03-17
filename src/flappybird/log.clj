(ns flappybird.log)

(defmacro info [args]
  `(apply js/console.info (to-array (concat ~[args]))))

(defmacro debug [args]
  `(apply js/console.debug (to-array (concat ~[args]))))

(defmacro error [args]
  `(apply js/console.error (to-array (concat ~[args]))))

(defmacro warn [args]
  `(apply js/console.warn (to-array (concat ~[args]))))




