package mocket.instrument;

import mocket.Configuration;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ActionMethodScanner extends MethodVisitor {

    MethodVisitor mv;

    public ActionMethodScanner(MethodVisitor methodVisitor) {
        super(Configuration.ASM_VERSION);
        mv = methodVisitor;
    }

    private boolean isAction = false;
    private String actionName = "";

    public boolean isAction() {
        return (isAction || !actionName.equals(""));
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        ActionNameScanner av = new ActionNameScanner();
        return av;
    }

    @Override
    public void visitCode() {

        super.visitCode();
    }

    @Override
    public void visitEnd() {
        if(isAction()) {
            // There should be an Integer actionId in the stack
            super.visitLdcInsn(actionName);
            super.visitMethodInsn(Opcodes.INVOKESTATIC, "mocket/instrument/runtime/Interceptor",
                    "checkState", "(Ljava/lang/String;I)B", false);
        }
        super.visitEnd();
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        if(!actionName.equals("") && opcode == Opcodes.INVOKESTATIC
                && owner.equals("mocket/instrument/runtime/Interceptor")
                && name.equals("collectParams")) {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            /**
             * After executing {@link mocket.instrument.runtime.Interceptor#collectParams},
             * we should have an Integer actionId in the stack
             */
            super.visitInsn(Opcodes.DUP); // For state checking at the end of the method
            super.visitLdcInsn(actionName);
            super.visitMethodInsn(Opcodes.INVOKESTATIC, "mocket/instrument/runtime/Interceptor",
                    "notifyAndBlock", "(Ljava/lang/String;I)B", false);
        }
    }

    private class ActionNameScanner extends AnnotationVisitor {

        public ActionNameScanner() {
            super(Configuration.ASM_VERSION);
        }

        @Override
        public void visit(String name, Object value) {
            if(name.equals("Action")) {
                isAction = true;
                actionName = value.toString();
            }
        }
    }
}
