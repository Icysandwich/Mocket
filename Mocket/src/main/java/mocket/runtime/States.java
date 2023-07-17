package mocket.runtime;

import mocket.path.raft.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class States {
    Map<String, String> states;
    ArrayList<Message> msgs;

    public States(HashMap<String, String> map, ArrayList<Message> list) {
        if(map != null)
            this.states = map;
        else
            this.states = new HashMap<String, String>();
        if(msgs != null)
            this.msgs = list;
        else
            this.msgs = new ArrayList<Message>();
    }

    /**
     * Raft specified
     */
    public void initStates() {
        states.put("term_of_1", "0");
        states.put("term_of_2", "0");
        states.put("term_of_3", "0");
        states.put("vote_for_1", "0");
        states.put("vote_for_2", "0");
        states.put("vote_for_3", "0");
        states.put("vote_grant_11", "0");
        states.put("vote_grant_12", "0");
        states.put("vote_grant_13", "0");
        states.put("vote_grant_21", "0");
        states.put("vote_grant_22", "0");
        states.put("vote_grant_23", "0");
        states.put("vote_grant_31", "0");
        states.put("vote_grant_32", "0");
        states.put("vote_grant_33", "0");
        states.put("node_state_1", "Follower");
        states.put("node_state_2", "Follower");
        states.put("node_state_3", "Follower");
    }

    public Set<String> stateNames() {
        return states.keySet();
    }

    public String getStateValue(String stateName) {
        return states.get(stateName);
    }

    public void updateState(String stateName, Object value) {

    }
}
