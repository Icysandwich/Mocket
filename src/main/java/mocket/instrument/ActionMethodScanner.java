package mocket.instrument;

import mocket.Configuration;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public class ActionMethodScanner extends MethodVisitor {
    public ActionMethodScanner() {
        super(Configuration.ASM_VERSION);
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
        if(isAction()) {
            super.visitLdcInsn(actionName);
            super.visitMethodInsn(INVOKESTATIC, "mocket/instrument/runtime",
                    "Interceptor", "(Ljava/lang/String;I)B", false);
        }
        super.visitCode();
    }

    @Override
    public void visitEnd() {

        super.visitEnd();
    }

    private class ActionNameScanner extends AnnotationVisitor {

        public ActionNameScanner() {
            super(Configuration.ASM_VERSION);
        }

        @Override
        public void visit(String name, Object value) {
            if(name.equals("Action")) {
                isAction = true;
            }
        }
    }
}
