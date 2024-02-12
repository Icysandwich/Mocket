package mocket.instrument.runtime.raft;

import java.util.ArrayList;
import java.util.HashMap;

import mocket.path.raft.Message;

public class LocalState extends mocket.instrument.runtime.LocalState{


    public LocalState(HashMap<String, Object> stateMap) {
        if(stateMap != null)
            this.states = stateMap;
        else
            this.states = new HashMap<String, Object>();
        this.msgSent = new ArrayList<Message>();
        this.msgNotReceived = new ArrayList<Message>();
        initState();
    }

    protected void initState() {
        states.put("currentTerm", 0);
        states.put("votedFor", 0);
        states.put("voteGranted_1", false);
        states.put("voteGranted_2", false);
        states.put("voteGranted_3", false);
        states.put("state", "Follower");
    }

    public void updateState(String stateName, Object value) {
        states.put(stateName, value);
    }
}
