package mocket.instrument.runtime;

import java.util.HashMap;

public class Variables {
    private HashMap<String,Object> vars;
    public Variables() {
        vars = new HashMap<>();
    }
    public void updateValue(String varName, Object value) {
        vars.put(varName, value);
    }

    public boolean hasVariable(String varName) {
        return vars.containsKey(varName);
    }
}
