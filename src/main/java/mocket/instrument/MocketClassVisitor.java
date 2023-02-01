package mocket.instrument;

import mocket.Configuration;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public class MocketClassVisitor extends ClassVisitor {

    public MocketClassVisitor(ClassVisitor classVisitor) {
        super(Configuration.ASM_VERSION, classVisitor);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        VariableFieldScanner fv = new VariableFieldScanner();
        if(fv.isVariable()) {
            PreMain.vars.updateValue(fv.getVariableName(), value);
        }
        return fv;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        ActionMethodScanner mv = new ActionMethodScanner();
        return mv;
    }
}
