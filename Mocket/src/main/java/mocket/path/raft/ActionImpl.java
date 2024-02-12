package mocket.path.raft;

import mocket.path.Action;
import mocket.path.ActionType;

public class ActionImpl extends Action {

    public ActionImpl() {
        super();
    }

    public ActionImpl(ActionType type, int nid) {
        super(type, nid);
    }

    public ActionImpl(ActionType type, int sendNode, Message msg) {
        super(type, sendNode, msg);
    }

    public ActionImpl(ActionType type, Message msg, int receiveNode) {
        super(type, msg, receiveNode);
    }

    /**
     * Constructor for {@link mocket.runtime.testbed.ActionScheduler#getAction}
     * 
     * @param actionType
     * @param parameters
     */
    public ActionImpl(ActionType actionType, String[] parameters) {
        this.type = actionType;
        if (actionType == ActionType.RAFT_Timeout
                || actionType == ActionType.RAFT_BecomeLeader
                || actionType == ActionType.RAFT_Restart) {
            // Parameters: [NodeID:int]
            this.executedNode = Integer.parseInt(parameters[0]);
        } else if (actionType == ActionType.RAFT_RequestVote) {
            // Parameters: [SendNodeID:int, ReceiveNodeID:int, MessageType:String, term:int]
            this.executedNode = Integer.parseInt(parameters[0]);
            this.msg = new Message(parameters[2], this.executedNode,
                    Integer.parseInt(parameters[1]), Integer.parseInt(parameters[3]));
        } else if (actionType == ActionType.RAFT_HandleRequestVoteRequest) {
            // Parameters: []
            this.executedNode = Integer.parseInt(parameters[1]);
        } else if (actionType == ActionType.RAFT_HandleRequestVoteResponse) {
            // Parameters: [ReceieNodeID:int, SendNodeID:int, MessageType:String, term:int,
            // granted:boolean]
            this.executedNode = Integer.parseInt(parameters[0]);
            this.msg = new Message(parameters[2], this.executedNode,
                    Integer.parseInt(parameters[1]), Integer.parseInt(parameters[3]),
                    Boolean.parseBoolean(parameters[4]));
        }
    }

    public boolean isSingleNodeAction() {
        ActionType t = this.getActionType();
        if (t.equals(ActionType.RAFT_BecomeLeader) ||
                t.equals(ActionType.RAFT_Restart)) {
            return true;
        }
        return false;
    }

    public boolean isMessageRelatedAction() {
        ActionType t = this.getActionType();
        if (t.equals(ActionType.RAFT_RequestVote) ||
                t.equals(ActionType.RAFT_HandleRequestVoteRequest) ||
                t.equals(ActionType.RAFT_HandleRequestVoteResponse)) {
            return true;
        }
        return false;
    }

    public boolean isClientRequest() {
        return false;
    }

    public boolean isExternalFault() {
        return false;
    }
}
