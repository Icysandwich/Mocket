package mocket.instrument;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import mocket.Configuration;

public class VariableFieldScanner extends FieldVisitor {

    private boolean isVariable = false;

    private String TLAName = "";

    public VariableFieldScanner() {
        super(Configuration.ASM_VERSION);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return new VariableFieldValueScanner();
    }

    class VariableFieldValueScanner extends AnnotationVisitor {
        VariableFieldValueScanner() {
            super(Configuration.ASM_VERSION);
        }

        @Override
        public void visit(String name, Object value) {
            if(name.equals("Variable")) {
                isVariable = true;
                TLAName = value.toString();
                if(PreMain.vars.hasTLAVariable(TLAName)) {
                    System.out.println("[ERROR!] Duplicate TLA variable name:" + TLAName);
                }
                /**
                 * Record the field name: <TLA+ Variable name, Field name>.
                 * Field name is initialized in {@link MocketClassVisitor#visitField}.
                 */
                PreMain.vars.updateVariableName(TLAName, null);
            }
            super.visit(name, value);
        }
    }

    public boolean isVariable() {
        return isVariable;
    }

    public String getTLAName() {
        if(isVariable) {
            return TLAName;
        } else {
            return "";
        }
    }
}
