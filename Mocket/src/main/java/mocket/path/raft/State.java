package mocket.path.raft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class State implements mocket.path.State {

    String stateId;

    ArrayList<Message> msgs = new ArrayList<Message>();

    int[] currentTerms = new int[3];
    String[] votedFor = new String[3];
    boolean[][] votesGranted = new boolean[3][3];

    Map<String, String> nodeState = new HashMap<String, String>();

    public Message findConsumedMsg(State prevState) {
        ArrayList<Message> otherMsgs = prevState.msgs;
        for(Message anotherMsg: otherMsgs) {
            boolean consumed = true;
            for(Message msg: msgs) {
                if(msg.equals(anotherMsg)) {
                    consumed = false;
                    break;
                }
            }
            if(consumed)
                return anotherMsg;
        }
        return null;
    }

    @Override
    public boolean compareState(mocket.path.State s) {
        return false;
    }

    @Override
    public String getStateId() {
        return stateId;
    }
}
