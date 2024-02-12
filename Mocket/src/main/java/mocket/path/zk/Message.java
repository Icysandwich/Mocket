package mocket.path.zk;
public class Message extends mocket.path.Message{
    
    public Message(String messageType, int sourceNode, int destinationNode, int messageValue) {
        super(messageType, sourceNode, destinationNode, messageValue);
        //TODO Auto-generated constructor stub
    }

    String src;
    String dst;
    String leader;
    String state;
    String sid;

    /* Following fields not used for FLE now.
    *  They are all the same. */
    String type;
    long zxid;
    int electionEpoch;
    int peerEpoch;
    int num; // The number of this kind of msg

    public boolean equals(Message other) {
        if(dst.equals(other.dst) && src.equals(other.src)
                && leader.equals(other.leader) && state.equals(other.state)
                && sid.equals(other.sid))
            return true;
        else
            return false;
    }
}
