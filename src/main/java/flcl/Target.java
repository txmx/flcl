package flcl;

import org.jetbrains.annotations.Nullable;

public class Target {
    private final String name;
    private final int argCount;
    private final Class<?> returnType;
    private final Class<?>[] args;

    public Target(String name, @Nullable Class<?> returnType, int argCount, Class<?>[] args) {
        this.name = name;
        this.argCount = argCount;
        this.returnType = returnType == null ? Void.class : returnType;
        this.args = args;
    }

    public String getName() {
        return name;
    }

    public int getArgCount() {
        return argCount;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public Class<?>[] getArgTypes() {
        return args;
    }
}
