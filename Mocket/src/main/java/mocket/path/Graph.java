package mocket.path;

import java.io.IOException;
import java.util.ArrayList;

public abstract class Graph {
    public abstract ArrayList<Transition> readGraph(String edgePath, String nodePath) throws IOException;

    public String printPath(Transition root) {
        String ret = root.toString() + "\r\n";
        Transition next = root;
        while(next.hasNext()) {
            next = next.next;
            ret += next.toString() + "\r\n";
        }
        return ret;
    }
}
