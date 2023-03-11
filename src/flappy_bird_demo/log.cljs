(ns ekoontz.log)

;; thanks to all the commenters on
;; https://stackoverflow.com/questions/24239144/js-console-log-in-clojurescript

(defn info [& args]
  (.apply js/console.info js/console (to-array args)))

(defn debug [& args]
  (.apply js/console.debug js/console (to-array args)))

(defn error [& args]
  (.apply js/console.error js/console (to-array args)))

(defn warn [& args]
  (.apply js/console.warn js/console (to-array args)))


;; another possible set of implementations: (not sure which is better):

(comment

(defn fmt [msgs]
  (apply str (interpose " " (map pr-str msgs))))

(defn debug [& s]
  (let [msg (fmt s)]
    (js/console.debug msg)))

(defn error [& s]
  (let [msg (fmt s)]
    (js/console.error msg)))

(defn info [& s]
  (let [msg (fmt s)]
    (js/console.info msg)))

(defn warn [& s]
  (let [msg (fmt s)]
    (js/console.warn msg)))

)

