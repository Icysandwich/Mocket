package mocket.path;

public class Transition {
    public Transition next;
    public Transition prev;
    public int sid;
    public Action action; // The action that results in this state
    public State state;
    private boolean bChecked = false;
    private boolean sChecked = false;
//    String ID;

    public Transition(Transition prev, int sid, Action action, State state){
        this.state = state;
        this.sid = sid;
        this.action = action;
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
        return action.getType().equals(ActionType.NULL);
    }

    public boolean isActionExecuted() {
        return this.bChecked;
    }

    public void executeAction() {
        this.bChecked = true;
    }

    public boolean isStateChecked() {
        if (!bChecked)
            return false;
        // We do not check states after client requests and injected faults
        else if (action.isClientRequest() || action.isExternalFault())
            return bChecked;
        else
            return sChecked;
    }

    @Override
    public String toString(){
        if(isInitialState()) {
            return "Initial state";
        } else {
            switch (action.getType()){
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