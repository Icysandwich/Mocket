package mocket;

import mocket.path.Transition;

import java.util.ArrayList;
import java.util.HashMap;

public class PathManager {

    HashMap<Integer, Transition> allPaths;
    ArrayList<Integer> testedPath;
    int currentTestingPath;

    public PathManager(){
        allPaths = new HashMap<Integer, Transition>();
        testedPath = new ArrayList<Integer>();
        currentTestingPath = 0;
    }

    public void setPaths(HashMap<Integer, Transition> initStates) {
        this.allPaths = initStates;
    }

    public void addPath(int id, Transition initState) {
        if(allPaths == null) {
            throw new RuntimeException("PathManager is not initialized yet!");
        }
        if(allPaths.containsKey(id)) {
            throw new RuntimeException("Id: " + id + " is already used!");
        }
        allPaths.put(id, initState);
    }

    public void removePath(int id) {
        allPaths.remove(id);
        testedPath.add(id);
    }

    public int getCurrentTestingPathId() {
        return this.currentTestingPath;
    }

    public Transition getNextPath() {
        if(hasNextPath()) {
            currentTestingPath++;
            return allPaths.get(currentTestingPath);
        } else {
            return null;
        }
    }

    public boolean hasNextPath() {
        if(allPaths != null) {
            return !allPaths.isEmpty();
        }
        return false;
    }
}
