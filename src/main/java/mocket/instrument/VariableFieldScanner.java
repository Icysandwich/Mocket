package mocket.instrument;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import mocket.Configuration;

public class VariableFieldScanner extends FieldVisitor {

    private boolean isVariable = false;

    private String varName = "";

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
                varName = value.toString();
                if(PreMain.vars.hasVariable(varName)) {
                    System.out.println("[ERROR!] Duplicate variable name:" + varName);
                }
                PreMain.vars.updateValue(varName, null);
            } else {
                super.visit(name, value);
            }
        }
    }

    public boolean isVariable() {
        return isVariable;
    }

    public String getVariableName() {
        if(isVariable) {
            return varName;
        } else {
            return "";
        }
    }
}
