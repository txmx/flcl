(ns flcl.adapter
  (:gen-class
    :name flcl.adapter.ClojureAdapter
    :implements [net.fabricmc.loader.api.LanguageAdapter]
    :main false)
  (:require [clojure.string :as str]))

(defn -create [_this _mod value _type]
  (let [entry (str/split value #"/")
        splits (count entry)
        load-class
        (fn [ns name]
          (let [ns (symbol ns)
                name (symbol name)
                entrypoint (ns-resolve (doto ns require) name)]
            (if (var? entrypoint)
              (reify net.fabricmc.api.ModInitializer (onInitialize [_this] (entrypoint)))
              entrypoint)))]
    (cond
      ;(= 0) (throw (ClassNotFoundException. "no entrypoint provided"))
      (= 1 splits) (load-class (first entry) "mod-init")
      (= 2 splits) (load-class (first entry) (-> entry rest first)))))
