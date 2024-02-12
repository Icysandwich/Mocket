package mocket.instrument.runtime;

import java.util.HashMap;
import java.util.Set;

public class Variables {

    private HashMap<String,MappedVariable> vars;

    public Variables() {
        vars = new HashMap<>();
    }

    public void updateVariableName(String TLAName, MappedVariable mappedVariable) {
        vars.put(TLAName, mappedVariable);
    }

    public boolean hasTLAVariable(String TLAName) {
        return vars.containsKey(TLAName);
    }

    public boolean hasMappedVariable(MappedVariable mappedVariable) {
        return vars.containsValue(mappedVariable);
    }

    public Set<String> getTLANames() {
        return vars.keySet();
    }

    public MappedVariable getMappedVariable(String TLAName) {
        if (hasTLAVariable(TLAName)) 
            return vars.get(TLAName);
        else
            return null;
    }

}
