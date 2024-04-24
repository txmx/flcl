This is a **F**abric **L**anguage Adapter for **Cl**ojure.

# Usage
Mixins in Clojure are used almost the exact same way they're used in Java. More differences will arise once the code allows for more Clojure-fication of the macro. Targets must have their parameter types and return type defined concretely, exactly how the injection expects the layout to be. Note that the first parameter on non-static method targets will contain the `this` for your target class.
```clojure
(mixin MinecraftServer
       (:modify-arg ^{:at (at :new :target "net/minecraft/server/ServerMetadata")
                      :ordinal 0}
         ^Text createMetadata [self ^Text _] (Text/of "hello world")))
```
