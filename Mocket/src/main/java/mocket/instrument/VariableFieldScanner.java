package mocket.instrument;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import mocket.Configuration;
import mocket.instrument.runtime.MappedVariable;

public class VariableFieldScanner extends FieldVisitor {

    private ClassVisitor cv;
    private FieldVisitor fv;

    private String fieldName;
    private Object fieldValue;

    private String owner;
    private String descriptor;
    private String signature;

    private boolean isVariable = false;

    private String TLAName = "";

    public VariableFieldScanner(ClassVisitor cv, FieldVisitor fv, 
            String owner, String name, String descriptor, String signature, Object value) {
        super(Configuration.ASM_VERSION, fv);
        this.cv = cv;
        this.fv = fv;
        this.owner = owner;
        this.descriptor = descriptor;
        this.signature = signature;
        this.fieldName = name;
        this.fieldValue = value;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (desc.equals("Lmocket/annotation/MocketVariable;")) {
            isVariable = true;
            return new VariableFieldValueScanner();
        } else {
            return fv.visitAnnotation(desc, visible);
        }

    }

    class VariableFieldValueScanner extends AnnotationVisitor {
        VariableFieldValueScanner() {
            super(Configuration.ASM_VERSION);
        }

        @Override
        public void visit(String name, Object value) {
            isVariable = true;
            TLAName = value.toString();
            // Initialize and store field name.
            PreMain.vars.updateVariableName(TLAName, new MappedVariable(owner, fieldName, descriptor));
            /**
             * Create a shadow field for the mapped TLA+ variable,
             * so that we could collect variable values without affecting the system
             * execution.
             */
            cv.visitField(Opcodes.ACC_STATIC, "MOCKET$" + fieldName, descriptor, signature, fieldValue);
            super.visit(name, value);
        }
    }

    public boolean isVariable() {
        return isVariable;
    }

    public String getTLAName() {
        if (isVariable) {
            return TLAName;
        } else {
            return "";
        }
    }
}
