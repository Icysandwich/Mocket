package mocket.path.raft;

import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import mocket.path.ActionType;
import mocket.path.Graph;
import mocket.path.Transition;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GraphImpl implements Graph {

    public Map<String, State> nodes = new HashMap<String, State>();

    public static MutableGraph readDot(String dotFile) {
        try {
            return new Parser().read(new File(dotFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ArrayList<Transition> readGraph(String edgePath, String nodePath) {
        ArrayList<Transition> rootNodes = new ArrayList<Transition>();
        File edgeFile = new File(edgePath);
        File nodeFile = new File(nodePath);
        try{
            BufferedReader in_node = new BufferedReader(new FileReader(nodeFile));
            String str;
            while((str = in_node.readLine()) != null) {
                String ID = str.split("/\\\\")[0].trim();
                String state = str.substring(str.indexOf("messages"));
                State s = readState(state);
                nodes.put(ID, s);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try{
            BufferedReader in_edge = new BufferedReader(new FileReader(edgeFile));
            String str;
            while((str = in_edge.readLine()) != null) {
                String[] edges = str.split(" ");
                Transition root = new Transition(null, -1, new ActionImpl(), nodes.get(edges[0]));
                rootNodes.add(root);
                Transition prev = root;
                String behavior = "";
                int sid = -1;
//                Action behavior = Action.NULL;
                for (int i = 1; i < edges.length; i++) {
                    if (i % 2 == 0){ // is node ID
                        /**
                         * Next has two types of transitions:
                         * 1. HandleRequestVoteRequest.
                         * 2. HandleRequestVoteResponse.
                         */
                        if(behavior.equals("Next")) {
                            State s = nodes.get(edges[i]);
                            Message consumedMsg = s.findConsumedMsg((State)prev.getState());
                            if(consumedMsg.type.equals("RequestVoteRequest"))
                                behavior = "HandleRequestVoteRequest";
                            else if (consumedMsg.type.equals("RequestVoteResponse"))
                                behavior = "HandleRequestVoteResponse";
                            else
                                throw new Exception("Unknown message consuming behavior!");
                        }
                        prev = new Transition(prev, sid, new mocket.path.zk.ActionImpl(ActionType.valueOf(behavior), sid), nodes.get(edges[i]));
                    } else { // is behavior
                        behavior = edges[i];
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rootNodes;
    }

    private State readState(String state) throws Exception {
        State s = new State();
        String[] lines = state.replaceAll(" ", "").split("/\\\\");
        if(lines.length != 8)
            throw new Exception("Unexpected state format:" + state);
        String messages = lines[0];
        /**
         * Format of messages:
         * messages = ([mtype |-> RequestVoteRequest, mterm |-> 2, msource |-> s1,
         * mdest |-> s1] :> \n   1 @@\n [mtype |-> RequestVoteResponse,\n  mterm |-> 2,\n
         * msource |-> s1,\n  mdest |-> s1,\n mvoteGranted |-> TRUE] :> \n   0)
         */
        if(messages.contains("RequestVoteRequest")) {
            String[] reqMsgs = messages.split("mytype|->RequestVoteRequest");
            int num = reqMsgs.length;
            for (int i = 1; i < num; i++) {
                String msgString = reqMsgs[i];
                if(msgString.contains("@@"))
                    msgString = msgString.split("@@")[0];
                Message msg = new Message();

                String[] fields = msgString.split("\\|->");
                if(fields.length != 4 )
                    throw new Exception("Unexpected msg format:" + msgString);
                msg.type = "RequestVoteRequest";
                msg.val = Integer.parseInt(fields[1].split(",")[0]);
                msg.src = fields[2].split(",")[0];
                msg.dst = fields[3].split("]")[0];
                String numString = fields[3].split(":>")[1].split("\\)")[0];
                if(numString.contains("n"))
                    numString = numString.split("n")[1];
                msg.num = Integer.parseInt(numString);

                s.msgs.add(msg);
            }
        }
        if(messages.contains("RequestVoteResponse")) {
            String[] rspMsgs = messages.split("mytype|->RequestVoteResponse");
            int num = rspMsgs.length;
            for (int i = 1; i < num; i ++) {
                String msgString =  rspMsgs[i];
                Message msg = new Message();

                String[] fields = msgString.split("\\|->");
                if(fields.length != 5 )
                    throw new Exception("Unexpected msg format:" + msgString);
                msg.type = "RequestVoteResponse";
                msg.val = Integer.parseInt(fields[1].split(",")[0]);
                msg.src = fields[2].split(",")[0];
                msg.dst = fields[3].split(",")[0];
                // TODO: Does not consider mvoteGranted for now
                msg.num = Integer.parseInt(fields[4].split("n")[1].split("\\)")[0].split("@@")[0]);

                s.msgs.add(msg);
            }
        }

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
            s.nodeState.put(nodeAndState[0], nodeAndState[1]);
        }

        // TODO: Skip timeout num constraint for now.
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
            if(vgString.contains("s1"))
                s.votesGranted[i][0] = true;
            if(vgString.contains("s2"))
                s.votesGranted[i][1] = true;
            if(vgString.contains("s3"))
                s.votesGranted[i][2] = true;
        }

        // TODO: Skip votesSent variable for not using it
        /**
         * votedFor format:
         * votedFor = (s1 :> s1 @@ s2 :> s1 @@ s3 :> Nil)
         */
        String[] votedFor = lines[7].split("\\(")[1].split("@@");
        for (int i = 0; i < nodeNum; i++) {
            String vfString = votedFor[i].split(":>")[1];
            if(i == nodeNum - 1) {
                vfString = vfString.substring(0, vfString.length()-1);
            }
            s.votedFor[i] = vfString;
        }
        return s;
    }

    public int countPath(MutableGraph g) {
        return 0;
    }

    public void printPath(Transition root) {
        System.out.println(root);
        Transition next = root;
        while(next.hasNext()) {
            next = next.next;
            System.out.println(next);
        }
    }
}
