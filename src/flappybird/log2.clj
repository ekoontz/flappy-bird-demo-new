(ns flappybird.log2)

(defmacro info [args]
  `(apply js/console.info (to-array (concat ~[args]))))




