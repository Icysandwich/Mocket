package mocket.path;

public enum BehaviorType {
    UNKNOWN(-1),
    NULL(0), // Used when the node is the first state
    /* Raft behaviors */
    RAFT_Timeout(1), // Always the first behavior
    RAFT_RequestVote(2), // Generate a request from Server i to j
    RAFT_HandleRequestVoteRequest(3), // Consume a vote request, and generate a vote response
    RAFT_HandleRequestVoteResponse(4), // Consume a vote response
    RAFT_BecomeLeader(5), // Always the last behavior
    /* ZooKeeper Fast Leader Election behaviors */
    ZK_SendMessage(6), // SendWorker send out a message
    ZK_ReceiveMessage(7), // ReceiveWorker receive in a message
    ZK_HandleMessage(8); // Main while loop process a message

    private final int v;

    BehaviorType(final int value) {
        this.v = value;
    }

    public int getValue() { return v; }

    public static BehaviorType getBehaviorType(int value) {
        for(BehaviorType t : BehaviorType.values()) {
            if (t.v == value)
                return t;
        }
        return UNKNOWN;
    }
}
