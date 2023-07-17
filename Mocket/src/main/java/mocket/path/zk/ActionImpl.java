package mocket.path.zk;

import mocket.path.Action;
import mocket.path.ActionType;

import java.util.HashSet;

public class ActionImpl implements Action {

    ActionType type = ActionType.NULL;
    HashSet<String> parameters = null;

    /**
     * Initialized a ZK_ReceiveMessage behavior
     * @param m
     */
    public ActionImpl(Message m) {
        this.type = ActionType.ZK_ReceiveMessage;
        this.parameters = new HashSet<>();
    }

    /**
     * Initialized a ZK_SendMessage / ZK_HandleMessage behavior
     * @param type
     * @param sid
     */
    public ActionImpl(ActionType type, int sid) {
        this.type = type;
        HashSet<String> params = new HashSet<>();
        params.add(String.valueOf(sid));
        this.parameters = params;
    }

    @Override
    public ActionType getType() {
        return this.type;
    }

    @Override
    public HashSet<String> getParameters() {
        return parameters;
    }

    @Override
    public boolean compare(Action b) {
        return false;
    }

    @Override
    public boolean isSingleNodeAction() {
        return false;
    }

    @Override
    public boolean isMessageRelatedAction() {
        ActionType t = this.getType();
        if (t.equals(ActionType.ZK_SendMessage) ||
                t.equals(ActionType.ZK_HandleMessage) ||
                t.equals(ActionType.ZK_ReceiveMessage)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isClientRequest() {
        return false;
    }

    @Override
    public boolean isExternalFault() {
        return false;
    }
}
