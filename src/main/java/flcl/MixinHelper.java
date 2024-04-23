package flcl;

import clojure.lang.IFn;
import clojure.lang.Keyword;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;

// T - the mixin target
public class MixinHelper<T> {
    private static final String MIXIN_ANNOTATION = "Lorg/spongepowered/asm/mixin/Mixin;";

    protected final Class<T> targetClass;
    protected final ClassWriter asmWriter;

    public MixinHelper(Class<T> target) {
        targetClass = target;
        asmWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        // begin building class at construction. really consider alternatives.
        String mixinName = targetClass.getSimpleName() + "Mixin";
        asmWriter.visit(
                Opcodes.V17,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                mixinName,
                null,
                "java/lang/Object",
                null);
        AnnotationVisitor mixin = asmWriter.visitAnnotation(MIXIN_ANNOTATION, false);
        {
            AnnotationVisitor targetAtn = mixin.visitArray("value");
            targetAtn.visit(null, Type.getType(targetClass));
            targetAtn.visitEnd();
        }
    }

    private Method findMethod(Target target) {
        // THIS NEEDS TO BE IMPROVED TO ACTUALLY BE FUNCTIONAL
        try {
            return targetClass.getDeclaredMethod(target.getName());
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private void buildAnnotation(AnnotationVisitor annotation,
                                 Iterator<Map.Entry<Keyword, Object>> meta,
                                 boolean isArray)
    {
        if (meta == null) {
            return;
        }

        if (!meta.hasNext()) {
            return;
        }

        Map.Entry<Keyword, Object> entry = meta.next();
        String name = isArray ? null : entry.getKey().getName();
        Object value = entry.getValue();

        if (value.getClass().isArray()) {
            annotation.visitArray(name);
            buildAnnotation(annotation, meta, true);
        }
        else if (value instanceof AnnotationBuilder builder) {
            builder.build(annotation, name);
        }
        else {
            annotation.visit(name, value);
        }

        buildAnnotation(annotation, meta, isArray);
    }

    private static String targetDescriptor(Target target, Method method) {
        StringBuilder descriptor = new StringBuilder("(");

        Class<?>[] defaultTypes = method.getParameterTypes();
        Class<?>[] argTypes = target.getArgTypes();

        int index = Modifier.isStatic(method.getModifiers()) ? 0 : 1;
        for (int i = index; i < argTypes.length; ++i) {
            Class<?> type = argTypes[i];
            if (type.isInstance(Object.class) && i < defaultTypes.length) {
                type = defaultTypes[i];
            }
            descriptor.append(Type.getDescriptor(type));
        }
        descriptor.append(')');

        Class<?> retType = target.getReturnType();
        descriptor.append(retType.equals(Void.class) ? "V" : Type.getDescriptor(retType));

        return descriptor.toString();
    }

    public static int returnOpcodeFromClass(Class<?> any) {
        return switch (any.getSimpleName()) {
            case "Integer", "Boolean", "Character", "Short", "Byte" -> Opcodes.IRETURN;
            case "Long" -> Opcodes.LRETURN;
            case "Void" -> Opcodes.RETURN;
            default -> Opcodes.ARETURN;
        };
    }

    public String invocationDescriptor(Target target, Method method) {
        StringBuilder descriptor = new StringBuilder("(");

        int index = 0;
        if (!Modifier.isStatic(method.getModifiers())) {
            descriptor.append("Ljava/lang/Object;");
            ++index;
        }
        for (int i = index; i < target.getArgCount(); ++i) {
            descriptor.append("Ljava/lang/Object;");
        }
        descriptor.append(")Ljava/lang/Object;");

        return descriptor.toString();
    }

    public void visitInjection(Class<? extends Annotation> annotation,
                               @Nullable Map<Keyword, Object> meta,
                               Target target,
                               IFn code)
    {
        Method targetMethod = findMethod(target);
        if (targetMethod == null) {
            throw new IllegalArgumentException("Couldn't find method '" + target.getName() + "'");
        }

        MethodVisitor mixin = asmWriter.visitMethod(
                Opcodes.ACC_PRIVATE,
                target.getName(),
                targetDescriptor(target, targetMethod),
                null,
                null);

        AnnotationVisitor inject = mixin.visitAnnotation(Type.getDescriptor(annotation), true);
        {
            AnnotationVisitor methods = inject.visitArray("method");
            methods.visit(null, target.getName());
            methods.visitEnd();
        }
        buildAnnotation(inject, meta == null ? null : meta.entrySet().iterator(), false);
        inject.visitEnd();

        mixin.visitCode();
        mixin.visitMaxs(target.getArgCount() + 2, target.getArgCount() + 1);

        String invokeName = Type.getInternalName(code.getClass());
        mixin.visitTypeInsn(Opcodes.NEW, invokeName);
        mixin.visitInsn(Opcodes.DUP);
        mixin.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                invokeName,
                "<init>",
                "()V",
                false);

        int index = 0;
        if (!Modifier.isStatic(targetMethod.getModifiers())) {
            mixin.visitVarInsn(Opcodes.ALOAD, index);
            ++index;
        }
        for (int i = index; i < target.getArgCount(); ++i) {
            mixin.visitVarInsn(Opcodes.ALOAD, i);
        }
        mixin.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "clojure/lang/IFn",
                "invoke",
                invocationDescriptor(target, targetMethod),
                true);

        mixin.visitInsn(returnOpcodeFromClass(target.getReturnType()));
        mixin.visitEnd();
    }

    public void generate(String strPath) throws IOException {
        asmWriter.visitEnd();

        Path path = Paths.get(strPath, targetClass.getSimpleName() + "Mixin.class");
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        }
        Files.write(path, asmWriter.toByteArray());
    }
}
