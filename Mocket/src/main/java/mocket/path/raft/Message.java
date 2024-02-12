package mocket.path.raft;

public class Message extends mocket.path.Message{

    /**
     * Constrocutor for RequestVote message
     * @param messageType
     * @param sourceNode
     * @param destinationNode
     * @param term
     */
    public Message(String messageType, int sourceNode, int destinationNode, int term) {
        super(messageType, sourceNode, destinationNode, term);
        granted = false;
    }

    public Message(String messageType, int sourceNode, int destinationNode, int term, boolean isGranted) {
        super(messageType, sourceNode, destinationNode, term);
        granted = isGranted;
    }

    boolean granted;

    @Override
    public boolean equals(Object obj) {
        Message other = (Message) obj;
        return super.equals(other) 
                && (type.equals("ReceiveRequestVoteResponse") ? granted == other.granted : true);
        
    }

    public String toString() {
        return super.toString() + ", voteGranted: " + granted;
    }
}
