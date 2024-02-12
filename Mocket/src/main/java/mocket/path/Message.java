package mocket.path;

public abstract class Message {
    public String type;
    public int value;
    public int src;
    public int dst;

    public Message(String messageType, int sourceNode, int destinationNode, int messageValue) {
        type = messageType;
        src = sourceNode;
        dst = destinationNode;
        value = messageValue;
    }

    @Override
    public boolean equals(Object obj) {
        Message other = (Message) obj;
        if (other !=null)
            return type.equals(other.type) && dst == other.dst
                        && src == other.src && value == other.value;
        else
            return false;
    }

    public String toString() {
        return "type:" + type + ", sourceNode: " + src + ", destinationNode: " + dst + ", value: " + value;
    }
}
