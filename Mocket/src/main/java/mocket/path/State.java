package mocket.path;

public interface State {
    boolean compareState(State s);
    String getStateId();
}
