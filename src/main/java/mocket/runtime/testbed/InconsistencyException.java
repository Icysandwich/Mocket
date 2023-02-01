package mocket.runtime.testbed;

public class InconsistencyException extends RuntimeException {

    public InconsistencyException(String message, InconsistencyType type) {
        super(message);
    }
}
