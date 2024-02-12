package mocket.instrument.runtime;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import mocket.path.raft.Message;

public abstract class LocalState {
    protected Map<String, Object> states;
    protected ArrayList<Message> msgSent;
    protected ArrayList<Message> msgNotReceived;

    protected abstract void initState();
    public abstract void updateState(String stateName, Object value);

    public Set<String> stateNames() {
        return states.keySet();
    }

    public Object getStateValue(String stateName) {
        return states.get(stateName);
    }
}
