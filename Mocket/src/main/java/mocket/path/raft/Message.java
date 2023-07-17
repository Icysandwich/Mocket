package mocket.path.raft;

public class Message {
    String type;
    int val; // Term
    String src;
    String dst;
    int num; // The number of this kind of msg

    public boolean equals(Message other) {
        if(type.equals(other.type) && dst.equals(other.dst)
                && src.equals(other.src) && val == other.val && num == other.num)
            return true;
        else
            return false;
    }
}
