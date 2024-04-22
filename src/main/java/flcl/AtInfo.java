package flcl;

import clojure.asm.Type;
import org.objectweb.asm.AnnotationVisitor;
import org.spongepowered.asm.mixin.injection.At;

public record AtInfo(String value,
                     String id,
                     String slice,
                     At.Shift shift,
                     int by,
                     String[] args,
                     String target,
                     int ordinal,
                     int opcode,
                     boolean remap)
    implements AnnotationBuilder
{
    @Override
    public void build(AnnotationVisitor parent, String name) {
        AnnotationVisitor at = parent.visitAnnotation(name, Type.getDescriptor(At.class));
        at.visit("value", value);
        at.visit("id", id);
        at.visit("slice", slice);
        at.visitEnum("shift", Type.getDescriptor(At.Shift.class), switch (shift) {
            case NONE -> "NONE";
            case BY -> "BY";
            case AFTER -> "AFTER";
            case BEFORE -> "BEFORE";
        });
        at.visit("by", by);
        {
            AnnotationVisitor argz = at.visitArray("args");
            for (String arg : args) {
                argz.visit(null, arg);
            }
            argz.visitEnd();
        }
        at.visit("target", target);
        at.visit("ordinal", ordinal);
        at.visit("opcode", opcode);
        at.visit("remap", remap);
        at.visitEnd();
    }
}
