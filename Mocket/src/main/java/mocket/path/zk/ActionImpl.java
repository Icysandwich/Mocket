package mocket.path.zk;

import mocket.path.Action;
import mocket.path.ActionType;

public class ActionImpl extends Action {

    ActionImpl() {
        super();
    }

    ActionImpl(ActionType type, int nid) {
        super(type, nid);
    }

    ActionImpl(ActionType type, int sendNode, Message msg) {
        super(type, sendNode, msg);
    }


    ActionImpl(ActionType type, Message msg, int receiveNode) {
        super(type, msg, receiveNode);
    }


    public boolean isSingleNodeAction() {
        return false;
    }

    public boolean isMessageRelatedAction() {
        ActionType t = this.getActionType();
        if (t.equals(ActionType.ZK_SendMessage) ||
                t.equals(ActionType.ZK_HandleMessage) ||
                t.equals(ActionType.ZK_ReceiveMessage)) {
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
