package mocket.path;

public enum ActionType {
    UNKNOWN(-1, "Unknown"),
    NULL(0, "Root"), // Used when the node is the first state
    /* Raft behaviors */
    RAFT_Timeout(1, "Timeout"), // Always the first behavior
    RAFT_RequestVote(2, "RequestVote"), // Generate a request from Server i to j
    RAFT_HandleRequestVoteRequest(3, "ReceiveRequestVoteRequest"), // Consume a vote request, and generate a vote response
    RAFT_HandleRequestVoteResponse(4, "ReceiveRequestVoteResponse"), // Consume a vote response
    RAFT_BecomeLeader(5, "BecomeLeader"), // Always the last behavior
    RAFT_Restart(6, "Restart"), // Restart a node
    /* ZooKeeper Fast Leader Election behaviors */
    ZK_SendMessage(7, "SendMessage"), // SendWorker send out a message
    ZK_ReceiveMessage(8, "ReceiveMessage"), // ReceiveWorker receive in a message
    ZK_HandleMessage(9, "HandleMessage"); // Main while loop process a message

    private final int v;

    private final String t;

    ActionType(final int value, final String actionName) {
        this.v = value;
        this.t = actionName;
    }

    public int getValue() { return v; }

    public String getType() {
        return t;
    }

    public static ActionType getActionType(int value) {
        for(ActionType t : ActionType.values()) {
            if (t.v == value)
                return t;
        }
        return UNKNOWN;
    }

    public static ActionType getActionType(String actionName) {
        for(ActionType t : ActionType.values()) {
            if (t.t.equals(actionName))
                return t;
        }
        return UNKNOWN;
    }
}
