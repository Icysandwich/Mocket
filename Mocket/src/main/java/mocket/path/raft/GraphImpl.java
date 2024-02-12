package mocket.path.raft;

import mocket.path.ActionType;
import mocket.Util;
import mocket.path.Action;
import mocket.path.Graph;
import mocket.path.GraphException;
import mocket.path.Transition;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GraphImpl extends Graph {

    public Map<String, State> nodes = new HashMap<String, State>();

    public ArrayList<Transition> readGraph(String edgePath, String nodePath) throws IOException, GraphException {
        ArrayList<Transition> rootNodes = new ArrayList<Transition>();
        File edgeFile = new File(edgePath);
        File nodeFile = new File(nodePath);
        try{
            BufferedReader nodeBuffer = new BufferedReader(new FileReader(nodeFile));
            String str;
            while((str = nodeBuffer.readLine()) != null) {
                String ID = str.split("/\\\\")[0].trim();
                String state = str.substring(str.indexOf("messages"));
                State s = readState(state);
                nodes.put(ID, s);
            }
            nodeBuffer.close();
        } catch (IOException | GraphException e) {
            throw e;
        }

        try{
            BufferedReader edgeBuffer = new BufferedReader(new FileReader(edgeFile));
            String str;
            while((str = edgeBuffer.readLine()) != null) {
                String[] edges = str.split(" ");
                Transition root = new Transition(null, -1, new ActionImpl(), nodes.get(edges[0]));
                rootNodes.add(root);
                Transition prev = root;
                String actionName = "";
                // Action action;
                for (int i = 1; i < edges.length; i++) {
                    if (i % 2 == 0){ // Current String is node ID
                        State s = nodes.get(edges[i]);
                        int sid = getNidByComparingStates(actionName, (State) prev.getState(), s);
                        ActionType type = ActionType.getActionType(actionName);
                        Action action;
                        switch(actionName) {
                            case "Timeout":
                            case "BecomeLeader":
                            case "Restart":
                                action = new ActionImpl(type, sid);
                                break;
                            case "RequestVote":
                                Message generatedMsg = s.findGeneratedMessage((State)prev.getState());
                                action = new ActionImpl(type, sid, generatedMsg);
                                break;
                            case "ReceiveRequestVoteRequest":
                            case "ReceiveRequestVoteResponse":
                                Message consumedMsg = s.findConsumedMessage((State)prev.getState());
                                action = new ActionImpl(type, consumedMsg, sid);
                                break;
                            default:
                                action = new ActionImpl(ActionType.UNKNOWN, sid);
                        }
                        prev = new Transition(prev, sid, action, s);
                    } else { // Current String is action
                        actionName = edges[i];
                    }
                }
            }
            edgeBuffer.close();
        } catch (IOException | GraphException e) {
            throw e;
        }
        return rootNodes;
    }

    /**
    * Format of messages:
    * messages = ([mtype |-> RequestVoteRequest, mterm |-> 2, msource |-> s1,
    * mdest |-> s1] :> \n   1 @@\n [mtype |-> RequestVoteResponse,\n  mterm |-> 2,\n
    * msource |-> s1,\n  mdest |-> s1,\n mvoteGranted |-> TRUE] :> \n   0)
    * 
    * New format (Use set instead of bag, no multiple same messages):
    * messages = {[mtype |-> RequestVoteRequest, mterm |-> 2, msource |-> n1,
    * mdest |-> n1],\n [mtype |-> RequestVoteResponse,\n  mterm |-> 2,\n
    * msource |-> n1,\n  mdest |-> n1,\n mvoteGranted |-> TRUE]}
    */
    private ArrayList<Message> processMessages(String messagesString) throws GraphException{
        ArrayList<Message> ret = new ArrayList<Message>();
        String[] msgStrings = messagesString.split("mtype");
        for(int i = 1; i < msgStrings.length; i++) {
            String msgString = msgStrings[i];
            Message msg;
            String[] fields = msgString.split("\\|->");
            if (msgString.contains("RequestVoteRequest")) {
                if(fields.length != 5 )
                    throw new GraphException("Unexpected message format:" + msgString + 
                            " Message fields num: "+ fields.length);
                String type = "RequestVoteRequest";
                int term = Integer.parseInt(fields[2].split(",")[0]);
                int src = Util.getNodeId(fields[3].split(",")[0]);
                int dst = Util.getNodeId(fields[4].split("]")[0]);
                msg = new Message(type, src, dst, term);
            } else if (msgString.contains("RequestVoteResponse")) {
                if(fields.length != 6 )
                    throw new GraphException("Unexpected message format:" + msgString);
                String type = "RequestVoteResponse";
                int term = Integer.parseInt(fields[2].split(",")[0]);
                int src = Util.getNodeId(fields[3].split(",")[0]);
                int dst = Util.getNodeId(fields[4].split(",")[0]);
                boolean granted = fields[5].split("]")[0].equals("TRUE") ? true : false;
                msg = new Message(type, src, dst, term, granted);
            } else {
                throw new GraphException("Unexpected message format:" + msgString);
            }
            ret.add(msg);
        }
        return ret;
    }

    private State readState(String state) throws GraphException {
        State s = new State();
        String[] lines = state.replaceAll(" ", "").split("/\\\\");
        if(lines.length != 7)
            throw new GraphException("Unexpected state format:" + state);
        /**
         * State variables: messages, restartNum, state, timeoutNum, currentTerm,
         * votesGranted, votedFor
         */
        String messagesString = lines[0];
        s.msgs = processMessages(messagesString);

        /**
         * nodeStates format:
         * state = (s1 :> Candidate @@ s2 :> Follower @@ s3 :> Follower)\n/
         */
        String[] nodeStates = lines[2].split("\\(")[1].split("@@");
        int nodeNum = nodeStates.length;
        for (int i = 0; i < nodeNum; i++) {
            String stateString = nodeStates[i];
            if(i == nodeNum - 1) {
                stateString = stateString.split("\\)")[0];
            }
            String[] nodeAndState = stateString.split(":>");
            s.role[i] = nodeAndState[1];
        }

        /**
         * currentTerms format:
         * currentTerm = (s1 :> 1 @@ s2 :> 1 @@ s3 :> 1)\n/
         */
        String[] currentTerms = lines[4].split("\\(")[1].split("@@");
        for (int i = 0; i < nodeNum; i++) {
            String termString = currentTerms[i];
            if(i == nodeNum - 1) {
                termString = termString.split("\\)")[0];
            }
            s.currentTerms[i] = Integer.parseInt(termString.split(":>")[1]);
        }

        /**
         * votesGranted format:
         * votesGranted = (s1 :> {s1, s2} @@ s2 :> {} @@ s3 :> {})\n
         */
        String[] votesGranted = lines[5].split("\\(")[1].split("@@");
        for (int i = 0; i < nodeNum; i++) {
            String vgString = votesGranted[i].split(":>")[1];
            if(vgString.contains("n1"))
                s.votesGranted[i][0] = true;
            if(vgString.contains("n2"))
                s.votesGranted[i][1] = true;
            if(vgString.contains("n3"))
                s.votesGranted[i][2] = true;
        }

        /**
         * votedFor format:
         * votedFor = (s1 :> s1 @@ s2 :> s1 @@ s3 :> Nil)
         */
        String[] votedFor = lines[6].split("\\(")[1].split("@@");
        for (int i = 0; i < nodeNum; i++) {
            String vfString = votedFor[i].split(":>")[1];
            if(i == nodeNum - 1) {
                vfString = vfString.substring(0, vfString.length()-1);
            }
            s.votedFor[i] = vfString;
        }

        return s;
    }


    /**
     * TLC cannot dump action parameters to dot files for now, so we must get them
     * by comparing states. 
     * Note that TLC developers have the plan to do this automatically.
     * Refer to <a href="https://github.com/tlaplus/tlaplus/issues/807">Feature request: 
     * show arguments to actions in bad behavior</a>
     * @param actionName
     * @param prev
     * @param next
     * @return 
     */
    private int getNidByComparingStates(String actionName, State prev, State next) {
        if (next == null)
            return -1;

        if (actionName.equals("Timeout")) {
            // Timeout changes state[i] from Follower to Candidate
            String[] currentRoles = next.role;
            if (prev == null) { // Initial state
                for (int i = 0; i < currentRoles.length; i++) {
                    if (currentRoles[i].equals("Candidate"))
                        return i+1;
                }
            } else {
                String[] prevRoles = prev.role;
                for (int i = 0; i < prevRoles.length; i++) {
                    if (currentRoles[i].equals("Candidate") 
                            && !currentRoles[i].equals(prevRoles[i])) {
                                return i+1;
                            }
                }
                throw new GraphException("Wrong Timeout action state modification:" 
                        + prev == null? "Initial state" : prev.toString() + " -> " + next.toString());
            }
        } else if (actionName.equals("BecomeLeader")) {
            // BecomeLeader changes state[i] from Candidate to Leader
            String[] currentRoles = next.role;
            String[] prevRoles = prev.role;
            for (int i = 0; i < prevRoles.length; i++) {
                if (currentRoles[i].equals("Leader") 
                        && !currentRoles[i].equals(prevRoles[i])) {
                            return i+1;
                        }
            }
            throw new GraphException("Wrong BecomeLeader action state modification:" 
                    + prev.toString() + " -> " + next.toString());
        } else if (actionName.equals("RequestVote")) {
            // RequestVote generates a message from Node src
            Message generatedMessage = next.findGeneratedMessage(prev);
            return generatedMessage.src;
        } else if (actionName.equals("ReceiveRequestVoteRequest") ||
            actionName.equals("ReceiveRequestVoteResponse")) {
            // Consumes a message sent to Node dst
            Message consumedMessage = next.findConsumedMessage(prev);
            return consumedMessage.dst;
        } else if (actionName.equals("Restart")) {

        } 
        return -1;
    }
}
