package flcl;

import org.objectweb.asm.AnnotationVisitor;

public interface AnnotationBuilder {
    void build(AnnotationVisitor parent, String name);
}
