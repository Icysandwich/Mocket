package mocket.path;

public class Transition {
    public Transition next;
    public Transition prev;
    public int sid;
    public Behavior behavior; // The behavior that results in this state
    public State state;
    private boolean bChecked = false;
    private boolean sChecked = false;
//    String ID;

    public Transition(Transition prev, int sid, Behavior behavior, State state){
        this.state = state;
        this.sid = sid;
        this.behavior = behavior;
        if(prev != null) {
            this.prev = prev;
            prev.next = this;
        }
    }

    public boolean hasNext() {
        return next != null;
    }

    public State getState() {
        return this.state;
    }

    public boolean isInitialState() {
        return behavior.getType().equals(BehaviorType.NULL);
    }

    public boolean isBehaviorExecuted() {
        return this.bChecked;
    }

    public void executeBehavior() {
        this.bChecked = true;
    }

    public boolean isStateChecked() {
        if (!bChecked)
            return false;
        else
            return sChecked;
    }

    @Override
    public String toString(){
        if(isInitialState()) {
            return "Initial state";
        } else {
            switch (behavior.getType()){
                case RAFT_Timeout:
                    return "Timeout at node";
                case RAFT_RequestVote:
                    return "Send a vote request to";
                case RAFT_HandleRequestVoteRequest:
                    return "Reply a vote request";
                case RAFT_HandleRequestVoteResponse:
                    return "Receive a vote response";
                case RAFT_BecomeLeader:
                    return "Become the leader";
                case ZK_SendMessage:
                    return "SendWorker send out a message";
                case ZK_ReceiveMessage:
                    return "ReceiveWorker receive in a message";
                case ZK_HandleMessage:
                    return "Main while loop process a message";
                default:
                    return "ERROR! UNKNOWN BEHAVIOR!";
            }
        }
    }
}