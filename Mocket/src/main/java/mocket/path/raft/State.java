package mocket.path.raft;

import java.util.ArrayList;
import java.util.Arrays;

public class State implements mocket.path.State {

    String stateId;

    ArrayList<Message> msgs = new ArrayList<Message>();

    int[] currentTerms = new int[nodeNum];
    String[] role = new String[nodeNum];
    String[] votedFor = new String[nodeNum];
    boolean[][] votesGranted = new boolean[nodeNum][nodeNum]; // Only for leader

    int executedNode = -1;

    public Message findConsumedMessage(State prevState) {
        ArrayList<Message> otherMsgs = prevState.msgs;
        for(Message anotherMsg: otherMsgs) {
            boolean consumed = true;
            for(Message msg: msgs) {
                if (msg.equals(anotherMsg)) {
                    consumed = false;
                    break;
                }
            }
            if (consumed)
                return anotherMsg;
        }
        return null;
    }

    public Message findGeneratedMessage(State prevState) {
        ArrayList<Message> otherMsgs = prevState.msgs;
        for(Message msg: msgs) {
            boolean generated = true;
            for(Message anotherMsg: otherMsgs) {
                if (anotherMsg.equals(msg)) {
                    generated = false;
                    break;
                }
            }
            if (generated)
                return msg;
        }
        return null;
    }

    public State updateState(int sid, String[] stateValues) {
        State ret = copyState();
        for (String stateField : stateValues) {
            String varName = stateField.split(",")[0];
            String varValue = stateField.split(",")[1];
            if (varName.equals("state")) {
                ret.role[sid] = parseRoleName(varValue);
            } else if (varName.equals("currentTerm")) {
                ret.currentTerms[sid] = Integer.parseInt(varValue);
            } else if (varName.equals("votedFor")) {
                ret.votedFor[sid] = "n" + varValue;
            } else if (varName.startsWith("voteGranted")) {
                int grantedSid = Integer.parseInt(varName.split("_")[1]);
                ret.votesGranted[sid][grantedSid - 1] = Boolean.parseBoolean(varValue);
            }
        }
        return ret;
    }

    /**
     * 
     * @param implName Can be STATE_LEADER, STATE_FOLLOWER,
     * STATE_CANDIDATE, STATE_PER_CANDIDATE
     * @return Leader, Follower, Candidate
     */
    private String parseRoleName(String implName) {
        if (implName.equals("STATE_LEADER")) {
            return "Leader";
        } else if (implName.equals("STATE_FOLLOWER")) {
            return "Follower";
        } else if (implName.equals("STATE_CANDIDATE") || implName.equals("STATE_PER_CANDIDATE")){
            return "Candidate";
        }
        return "";
    }

    @Override
    public boolean compareState(mocket.path.State s) {
        State newState = (State) s;
        return executedNode == newState.executedNode && Arrays.equals(currentTerms, newState.currentTerms)
                && Arrays.equals(role, newState.role)
                && Arrays.equals(votedFor, newState.votedFor)
                && Arrays.deepEquals(votesGranted, newState.votesGranted)
                && msgs.equals(newState.msgs);
    }

    @Override
    public String getStateId() {
        return stateId;
    }

    

    public String toString() {
        String ret = "{";
        ret += "currentTerm:[";
        for (int term : currentTerms) {
            ret += term + ",";
        }
        ret += "],state:[";
        for (String r : role) {
            ret += r + ",";
        }
        ret += "],votedFor:[";
        for (String vf : votedFor) {
            ret += vf + ",";
        }
        ret += "],votesGranted:[";
        for (int i = 0; i < nodeNum; i++) {
            ret += "[";
            for (int j = 0; j < nodeNum; j++) {
                ret += votesGranted[i][j] + ",";
            }
            ret += "],";
        }
        ret += "]}";
        return ret;
    }

    @Override
    public State copyState() {
        State s = new State();
        s.executedNode = executedNode;
        System.arraycopy(currentTerms, 0, s.currentTerms, 0, nodeNum);
        System.arraycopy(role, 0, s.role, 0, nodeNum);
        System.arraycopy(votedFor, 0, s.votedFor, 0, nodeNum);
        s.votesGranted = Arrays.stream(votesGranted).toArray(boolean[][]::new);
        return s;
    }
}
