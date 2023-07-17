package mocket.runtime.testbed;

public class InconsistencyException extends RuntimeException {

    private InconsistencyType type;
    private String location;
    private String time;

    public InconsistencyException(String message, InconsistencyType type, String location, String time) {
        super(message);
        this.type = type;
        this.location = location;
        this.time = time;
    }

    public String toString() {
        return "Type: " + this.type.getString() + "\r\n" +
                "Location: " + this.location + "\r\n" +
                "Time: " + this.time;
    }

    public String getTime() {
        return this.time;
    }
}
