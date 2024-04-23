(ns flcl.mixins
  (:require clojure.stacktrace)
  (:import (flcl AtInfo MixinHelper Target)
           (org.spongepowered.asm.mixin.injection At$Shift Inject ModifyArg ModifyArgs ModifyConstant ModifyVariable Redirect)))

(def ^:dynamic *injectors* (atom {:inject          Inject
                                  :modify-arg      ModifyArg
                                  :modify-args     ModifyArgs
                                  :modify-constant ModifyConstant
                                  :modify-variable ModifyVariable
                                  :redirect        Redirect}))

(defn injector [injector-keyword annotation] (swap! *injectors* #(assoc % injector-keyword annotation)))

(defn at [value & opts]
  (let [values {:head "HEAD"
                :tail "TAIL"
                :return "RETURN"
                :invoke "INVOKE"
                :invoke-assign "INVOKE_ASSIGN"
                :field "FIELD"
                :new "NEW"
                :invoke-string "INVOKE_STRING"
                :jump "JUMP"
                :constant "CONSTANT"}
        defaults {:value (get values value "INVALID")
                  :id    ""
                  :slice ""
                  :shift At$Shift/NONE :by 0
                  :args []
                  :target ""
                  :ordinal -1
                  :opcode -1
                  :remap true}
        options (merge defaults (apply hash-map opts))]
    (AtInfo.
          (:value options)
          (:id options)
          (:slice options)
          (:shift options)
          (int (:by options))
          (into-array String (:args options))
          (:target options)
          (int (:ordinal options))
          (int (:opcode options))
          (:remap options))))

(defn- target [name args]
  (let [return-type (:tag (meta name) 'Void/TYPE)
        arg-types (map #(resolve (:tag (meta %) 'Object)) args)]
    (new Target (str name) (resolve return-type) (count args) (into-array arg-types))))

(defn- emit-injection [helper mixin]
  (let [[annotation-type target-name target-args & body] mixin]
    (-> helper
        (.visitInjection (get @*injectors* annotation-type)
                         ; only have to do this because i hjave no fucking clue what im doing
                         (apply hash-map (flatten (map (fn [[key value]]
                                                         (let [head (if (seq? value) (first value) value)
                                                               f (if (symbol? head) (resolve head) head)]
                                                           (cond
                                                             (integer? value) [key (int value)]
                                                             (list? value) (if (and (symbol? head) (fn? @f))
                                                                             [key (apply f (rest value))]
                                                                             [key value])
                                                             :else [key value])))
                                                       (dissoc (meta target-name) :tag))))
                         (target target-name target-args)
                         (eval `(fn ~target-args ~@body))))))

(defn- emit-mixins [helper mixins]
  (loop [mixins mixins]
    (when (not (nil? mixins))
      (emit-injection helper (first mixins))
      (recur (next mixins)))))

(defmacro mixin [target & methods]
  (let [helper (new MixinHelper (resolve target))
        outpath (clojure.string/replace (str *ns*) #"\." "/")]
    (try (emit-mixins helper methods) (catch Throwable e (println e)))
    ;(emit-mixins helper methods)
    (-> helper (.generate (str *compile-path* "/" outpath)))))
