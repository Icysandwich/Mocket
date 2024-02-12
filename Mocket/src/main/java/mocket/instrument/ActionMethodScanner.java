package mocket.instrument;

import mocket.Configuration;
import mocket.instrument.runtime.MappedVariable;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public class ActionMethodScanner extends AdviceAdapter {

    MethodVisitor mv;

    public ActionMethodScanner(MethodVisitor methodVistor, int access, String name, String desc) {
        super(Configuration.ASM_VERSION, methodVistor, access, name, desc);
        mv = methodVistor;
    }

    private boolean isAction = false;
    private boolean parameterCollected = false;
    private String actionName = "";

    private int lvId = -1;

    public boolean isAction() {
        return (isAction || !actionName.equals(""));
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (desc.equals("Lmocket/annotation/MocketAction;")) {
            isAction = true;
            return new ActionNameScanner();
        } else {
            return mv.visitAnnotation(desc, visible);
        }
    }

    @Override
    public void onMethodExit(int opcode) {
        if(isAction() && parameterCollected && lvId >= 0) {
            /**
             * Update values of mapped variables
             */
            for (String TLAName : PreMain.vars.getTLANames()) {
                MappedVariable var = PreMain.vars.getMappedVariable(TLAName);
                mv.visitFieldInsn(Opcodes.GETSTATIC, "mocket/instrument/runtime/Interceptor", 
                        "state", "Lmocket/instrument/runtime/LocalState;");
                mv.visitLdcInsn(TLAName);
                mv.visitFieldInsn(Opcodes.GETSTATIC, var.getOwner(), "MOCKET$"+var.getName(), var.getDesc());
                if (Instrumenter.isPrimitiveTypes(var.getDesc())) {
                    String boxingType = Instrumenter.getBoxingTypeForPrimitiveType(var.getDesc());
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, boxingType, 
                            "valueOf", "("+var.getDesc()+")"+ Type.getObjectType(boxingType), false);
                }
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "mocket/instrument/runtime/LocalState", 
                        "updateState", "(Ljava/lang/String;Ljava/lang/Object;)V", false);
            }

            /**
             * Invoke {@link mocket.instrument.runtime.Interceptor#checkState}
             */
            mv.visitLdcInsn(actionName);
            // Stack: [acationName]
            mv.visitVarInsn(Opcodes.ILOAD, lvId);
            // Stack: [actionId, actionName]
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "mocket/instrument/runtime/Interceptor",
                    "checkState", "(Ljava/lang/String;I)Z", false);
            mv.visitInsn(Opcodes.POP);
        }
        super.onMethodExit(opcode);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        if(!actionName.equals("") && opcode == Opcodes.INVOKESTATIC
                && owner.equals("mocket/instrument/runtime/Interceptor")
                && name.equals("collectParams")) {
            parameterCollected = true;

            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            /**
             * After executing {@link mocket.instrument.runtime.Interceptor#collectParams},
             * we should have an Integer actionId in the stack
             */
            // Stack: [actionId]
            lvId = super.newLocal(Type.INT_TYPE);
            mv.visitVarInsn(Opcodes.ISTORE, lvId);
            // Stack: []
            mv.visitLdcInsn(actionName);
            // Stack: [acationName]
            mv.visitVarInsn(Opcodes.ILOAD, lvId);
            // Stack: [actionId, actionName]
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "mocket/instrument/runtime/Interceptor",
                    "notifyAndBlock", "(Ljava/lang/String;I)Z", false);
        } else {
            mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }

    private class ActionNameScanner extends AnnotationVisitor {

        public ActionNameScanner() {
            super(Configuration.ASM_VERSION);
        }

        @Override
        public void visit(String name, Object value) {
            actionName = value.toString();
            super.visit(name, value);
        }
    }
}
