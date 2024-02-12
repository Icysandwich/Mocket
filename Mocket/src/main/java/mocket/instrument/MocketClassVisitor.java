package mocket.instrument;

import mocket.Configuration;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * Instrumenting codes for:
 *  1. Identify annotations {@link mocket.annotation.MocketAction} and {@link mocket.annotation.MocketVariable}.
 *  2. Add {@link mocket.instrument.runtime.Interceptor#notifyAndBlock(String, int)} and
 *  {@link mocket.instrument.runtime.Interceptor#checkState(String, int)} for annotated methods.
 *  3. Add a shadow variable for annotated fields.
 *  4. Assign the same value to the shadow variables when annotated fields are modified.
 */
public class MocketClassVisitor extends ClassVisitor {

    private ClassVisitor cv;
    String cn;

    public MocketClassVisitor(ClassVisitor classVisitor, String className) {
        super(Configuration.ASM_VERSION, classVisitor);
        cv = classVisitor;
        this.cn = className;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        FieldVisitor fv = super.visitField(access, name, descriptor, signature, value);
        return new VariableFieldScanner(cv, fv, cn, name, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        // Instrument methods that are mapped by TLA+ actions
        ActionMethodScanner amv = new ActionMethodScanner(mv, access, name, descriptor);
        // Add shadow variable value propagation code for all methods
        ShadowVariableMethodScanner svmv = new ShadowVariableMethodScanner(amv);
        return svmv;
    }
}
