(ns ekoontz.log)

(defn info [& args]
  (.apply js/console.info js/console (to-array args)))
