package mocket.path.raft;

import mocket.path.Behavior;
import mocket.path.BehaviorType;

import java.util.HashSet;

public class BehaviorImpl implements Behavior {
    @Override
    public BehaviorType getType() {
        return null;
    }

    @Override
    public HashSet<String> getParameters() {
        return null;
    }

    @Override
    public boolean compare(Behavior b) {
        return false;
    }
}
