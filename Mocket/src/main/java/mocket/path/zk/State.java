package mocket.path.zk;

import mocket.Mocket;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class State implements mocket.path.State {

    String stateId;
    int nodeNum = Mocket.nodeNum;

    public class Vote{
        String leader;
        long zxid;
        int electionEpoch;
        String state;
        int peerEpoch;
    }

    public class Notification{
        String leader;
        long zxid;
        int electionEpoch;
        String state;
        String sid;
        int peerEpoch;
    }

    ArrayList<Message> messages = new ArrayList<Message>();
    Vote[] currentVote;
    String[] state;
    int[] logicallock;
    int[] proposedEpoch;
    long[] proposedZxid;
    String[] proposedLeader;
    Queue<Notification>[] sendqueue;
    Queue<Notification>[] recvqueue;

    Set<Vote>[] outofelection;
    Set<Vote>[] recvset;


    public State() {
        stateId = "0";
        currentVote = new Vote[nodeNum];
        state = new String[nodeNum];
        logicallock = new int[nodeNum];
        proposedEpoch = new int[nodeNum];
        proposedZxid = new long[nodeNum];
        proposedLeader = new String[nodeNum];
        sendqueue = new Queue[nodeNum];
        recvqueue = new Queue[nodeNum];
        for(int i = 0; i<=nodeNum; i++) {
            sendqueue[i] = new LinkedList<>();
            recvqueue[i] = new LinkedList<>();
        }
    }



    @Override
    public boolean compareState(mocket.path.State s) {
        return false;
    }

    @Override
    public String getStateId() {
        return stateId;
    }
}
