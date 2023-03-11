(ns ekoontz.log)

(defn info [& args]
  (.apply js/console.info js/console (to-array args)))

(defn debug [& args]
  (.apply js/console.debug js/console (to-array args)))

(defn error [& args]
  (.apply js/console.error js/console (to-array args)))

(defn warn [& args]
  (.apply js/console.warn js/console (to-array args)))
