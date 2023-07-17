package mocket.path.raft;

import mocket.path.Action;
import mocket.path.ActionType;

import java.util.HashSet;

public class ActionImpl implements Action {
    @Override
    public ActionType getType() {
        return null;
    }

    @Override
    public HashSet<String> getParameters() {
        return null;
    }

    @Override
    public boolean compare(Action b) {
        return false;
    }

    @Override
    public boolean isSingleNodeAction() {
        if(this.getType().equals(ActionType.RAFT_BecomeLeader)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isMessageRelatedAction() {
        ActionType t = this.getType();
        if (t.equals(ActionType.RAFT_RequestVote) ||
                t.equals(ActionType.RAFT_HandleRequestVoteRequest) ||
                t.equals(ActionType.RAFT_HandleRequestVoteResponse)) {
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
