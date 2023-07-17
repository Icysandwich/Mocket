package mocket.instrument;

import mocket.Configuration;
import mocket.instrument.PreMain;
import org.objectweb.asm.*;

public class ShadowVariableMethodScanner extends MethodVisitor{
    MethodVisitor mv;

    public ShadowVariableMethodScanner(MethodVisitor methodVisitor) {
        super(Configuration.ASM_VERSION, methodVisitor);
        mv = methodVisitor;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        // Check if the visited Field is mapped.
        if (PreMain.vars.hasMappedVariable(name + descriptor) && opcode == Opcodes.PUTFIELD) {
            super.visitInsn(Opcodes.DUP);
            super.visitFieldInsn(opcode, owner, name, descriptor);
            // Assign the same value to the shadow field
            super.visitFieldInsn(Opcodes.PUTSTATIC, owner, "MOCKET$"+name, descriptor);
        } else {
            super.visitFieldInsn(opcode, owner, name, descriptor);
        }
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
        super.visitLocalVariable(name, descriptor, signature, start, end, index);
        //TODO: instrumentation for method variables by the configuration file.
    }

    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end,
                                                          int[] index, String descriptor, boolean visible) {
        //TODO: instrumentation for method variables by local variable annotations.
        return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible);
    }
}
