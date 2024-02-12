package mocket.path;

import java.util.HashSet;

/**
 * TLA+ actions have three types of parameters:
 * 1. Action(Node): a single-node action.
 * 2. Action(Node1, message): Message-sending action: Node1 sends out a message.
 * 3. Action(message): Message-receiving action: a out-of-standing message is consumed.
 */
public abstract class Action {

    protected ActionType type;
    protected int executedNode;
    
    // It is the message sending node for message-sending aciton; 
    // otherwise the message receiving node.
    protected int msgNodeId;
    protected Message msg;

    /**
     * Initialize a null action for starting state
     * @param type
     */
    public Action() {
        type = ActionType.NULL;
        executedNode = -1;
        msgNodeId = -1;
        msg = null;
    }

    /**
     * Sing-node action constructor
     * @param actionType
     * @param nid
     */
    public Action(ActionType actionType, int nid) {
        type = actionType;
        executedNode = nid;
        msgNodeId = -1;
        msg = null;
    }

    /**
     * Message sending action constructor
     * @param actionType
     * @param sidsend
     * @param sidreceive
     */
    public Action(ActionType actionType, int sendNode, Message message) {
        type = actionType;
        executedNode = sendNode;
        msgNodeId = sendNode;
        msg = message;
        if (message == null) {
            throw new GraphException("Incorrect action construction: message is null.");
        }
        if (msgNodeId != message.src) {
            throw new GraphException("Incorrect action construction: sendNode=" + msgNodeId 
                    + " does not match the message.src=" + message.src);
        }
    }

    public Action(ActionType actionType, Message message, int receiveNode) {
        type = actionType;
        executedNode = receiveNode;
        msg = message;
        msgNodeId = receiveNode;
        if (message == null) {
            throw new GraphException("Incorrect action construction: message is null.");
        }
        if (msgNodeId != message.dst) {
            throw new GraphException("Incorrect action construction: receiveNode=" + msgNodeId 
                    + " does not match the message.dst=" + message.dst);
        }
    }

    public ActionType getActionType() {
        return this.type;
    }

    public Object getParameters() {
        if (this.isSingleNodeAction()) {
            return executedNode;
        } else if (this.isMessageRelatedAction()) {
            
        }
        return null;
    }

    public int getSid() {
        return this.executedNode;
    }

    public boolean compare(Action a) {
        if (type.equals(a.getActionType()) && executedNode == a.executedNode
                    && msgNodeId == a.msgNodeId) {
            if (msg == null) {
                return a.msg == null;
            } else {
                return msg.equals(a.msg);
            }
        }
        return false;
    }

    public abstract boolean isSingleNodeAction();
    public abstract boolean isMessageRelatedAction();
    public abstract boolean isClientRequest();
    public abstract boolean isExternalFault();

}
