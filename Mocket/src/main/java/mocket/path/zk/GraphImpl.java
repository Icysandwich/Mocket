package mocket.path.zk;

import mocket.path.Graph;
import mocket.path.Transition;
import mocket.path.ActionType;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GraphImpl extends Graph {

    public Map<String, State> nodes = new HashMap<String, State>();

    @Override
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
                ActionImpl init = new ActionImpl(ActionType.NULL, -1);
                Transition root = new Transition(null, -1, init, nodes.get(edges[0]));
                rootNodes.add(root);
                Transition prev = root;
                ActionImpl b = null;
                String behaviorType = "";
                int sid = -1;
                for (int i = 1; i < edges.length; i++) {
                    if (i % 2 == 0){ // is node ID
                        prev = new Transition(prev, sid, b, nodes.get(edges[i]));
                    } else { // is behavior
                        sid = -1; // TODO
                        behaviorType = edges[i];
                        if(behaviorType.equals("Next")) behaviorType = "ReceiveMessage";
                        b = new ActionImpl(ActionType.valueOf("ZK_" + behaviorType), sid);
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
        return null;
    }
}
