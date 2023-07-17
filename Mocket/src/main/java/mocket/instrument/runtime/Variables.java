package mocket.instrument.runtime;

import java.util.HashMap;

public class Variables {
    private HashMap<String,String> vars;
    public Variables() {
        vars = new HashMap<>();
    }
    public void updateVariableName(String TLAName, String varName) {
        vars.put(TLAName, varName);
    }

    public boolean hasTLAVariable(String TLAName) {
        return vars.containsKey(TLAName);
    }

    public boolean hasMappedVariable(String varName) {
        return vars.containsValue(varName);
    }

}
