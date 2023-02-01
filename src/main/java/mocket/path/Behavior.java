package mocket.path;

import java.util.HashSet;

public interface Behavior {
    public BehaviorType getType();
    public HashSet<String> getParameters();
    public boolean compare(Behavior b);
}
