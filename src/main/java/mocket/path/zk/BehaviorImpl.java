package mocket.path.zk;

import mocket.path.Behavior;
import mocket.path.BehaviorType;

import java.util.HashSet;

public class BehaviorImpl implements Behavior {

    BehaviorType type = BehaviorType.NULL;
    HashSet<String> parameters = null;

    /**
     * Initialized a ZK_ReceiveMessage behavior
     * @param m
     */
    public BehaviorImpl(Message m) {
        this.type = BehaviorType.ZK_ReceiveMessage;
        this.parameters = new HashSet<>();
    }

    /**
     * Initialized a ZK_SendMessage / ZK_HandleMessage behavior
     * @param type
     * @param sid
     */
    public BehaviorImpl(BehaviorType type, int sid) {
        this.type = type;
        HashSet<String> params = new HashSet<>();
        params.add(String.valueOf(sid));
        this.parameters = params;
    }

    @Override
    public BehaviorType getType() {
        return this.type;
    }

    @Override
    public HashSet<String> getParameters() {
        return parameters;
    }

    @Override
    public boolean compare(Behavior b) {
        return false;
    }
}
