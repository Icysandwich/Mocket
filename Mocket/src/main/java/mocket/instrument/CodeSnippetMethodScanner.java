package mocket.instrument;

import mocket.Configuration;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class CodeSnippetMethodScanner extends MethodVisitor {

    MethodVisitor mv;

    String actionName = "";

    public CodeSnippetMethodScanner(MethodVisitor methodVisitor) {
        super(Configuration.ASM_VERSION, methodVisitor);
        mv = methodVisitor;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        if(actionName.equals("") && opcode == Opcodes.INVOKESTATIC
                && owner.equals("mocket/instrument/runtime/Interceptor")
                && name.equals("beginAction")) {

        } else if (!actionName.equals("") && opcode == Opcodes.INVOKESTATIC
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
        } else if (!actionName.equals("") && opcode == Opcodes.INVOKESTATIC
                && owner.equals("mocket/instrument/runtime/Interceptor")
                && name.equals("endAction")) {
            // There should be an Integer actionId in the stack
            super.visitLdcInsn(actionName);
            super.visitMethodInsn(Opcodes.INVOKESTATIC, "mocket/instrument/runtime/Interceptor",
                    "checkState", "(Ljava/lang/String;I)B", false);
        } else {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }
}
