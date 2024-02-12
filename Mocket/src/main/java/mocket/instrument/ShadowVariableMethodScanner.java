package mocket.instrument;

import mocket.Configuration;
import mocket.instrument.runtime.MappedVariable;

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
        MappedVariable mappedVariable = new MappedVariable(owner, name, descriptor);
        if (PreMain.vars.hasMappedVariable(mappedVariable) && opcode == Opcodes.PUTFIELD) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn("Hello!");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            if (Type.getType(descriptor).getSize() == 2) { // For long and double
                // stack: [value, value, obj]
                mv.visitInsn(Opcodes.DUP2_X1);
                // stack: [value, value, obj, value, value]
            } else { // Other types
                // stack: [value, obj]
                mv.visitInsn(Opcodes.DUP_X1);
                // stack: [value, obj, value]
            }
            mv.visitFieldInsn(opcode, owner, name, descriptor);
            // stack: [value] or [value, value]
            // Assign the same value to the shadow field
            mv.visitFieldInsn(Opcodes.PUTSTATIC, owner, "MOCKET$"+name, descriptor);
        } else {
            mv.visitFieldInsn(opcode, owner, name, descriptor);
        }
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
        mv.visitLocalVariable(name, descriptor, signature, start, end, index);
        //TODO: instrumentation for method variables by the configuration file.
    }

    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end,
                                                          int[] index, String descriptor, boolean visible) {
        //TODO: instrumentation for method variables by local variable annotations.
        return mv.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible);
    }
}
