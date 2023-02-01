package mocket.path;

import guru.nidi.graphviz.model.MutableGraph;

import java.util.ArrayList;

public interface Graph {
    ArrayList<Transition> readGraph(String edgePath, String nodePath);
    int countPath(MutableGraph g);
    void printPath(Transition root);
}
