package mocket.path;

import mocket.Mocket;

public interface State {
    int nodeNum = Mocket.nodeNum;
    boolean compareState(State s);
    State updateState(int sid, String[] stateValues);
    String getStateId();
    State copyState();
}
