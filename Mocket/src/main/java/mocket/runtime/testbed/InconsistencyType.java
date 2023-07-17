package mocket.runtime.testbed;

public enum InconsistencyType {
    missing_action("Missing action"), unexpected_action("Unexpected action"), incorrect_state("Incorrect state");

    private String type;
    InconsistencyType(String type) {
        this.type = type;
    }

    public String getString() {
        return type;
    }
}
