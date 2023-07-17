package mocket.instrument;

import mocket.Configuration;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MocketClassVisitor extends ClassVisitor {

    private ClassVisitor cv;
    public MocketClassVisitor(ClassVisitor classVisitor) {
        super(Configuration.ASM_VERSION, classVisitor);
        cv = classVisitor;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        VariableFieldScanner fv = new VariableFieldScanner();
        if(fv.isVariable()) {
            // Initialize and store field name.
            PreMain.vars.updateVariableName(fv.getTLAName(), name + descriptor);
            /**
             * Create a shadow field for the mapped TLA+ variable,
             * so that we could collect variable values without affecting the system execution.
             */
            cv.visitField(Opcodes.ACC_STATIC, "MOCKET$"+name, descriptor, signature, value);
        }
        return fv;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        // Instrument methods that are mapped by TLA+ actions
        ActionMethodScanner amv = new ActionMethodScanner(super.visitMethod(access,name,descriptor,signature,exceptions));
        // Add shadow variable value propagation code for all methods
        ShadowVariableMethodScanner svmv = new ShadowVariableMethodScanner(amv);
        return svmv;
    }
}
