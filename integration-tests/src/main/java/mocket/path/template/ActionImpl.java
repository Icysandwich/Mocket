package mocket.path.template;

import mocket.path.Action;
import mocket.path.ActionType;

import java.util.HashSet;

public class ActionImpl implements Action {

    ActionType type = ActionType.NULL;
    HashSet<String> parameters = null;

    public ActionImpl(ActionType type) {
        this.type = type;
        HashSet<String> params = new HashSet<>();
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
