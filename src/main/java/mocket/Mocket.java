package mocket;

import mocket.faults.Fault;
import mocket.path.Graph;
import mocket.path.Transition;
import mocket.runtime.testbed.BehaviorController;
import mocket.runtime.testbed.Client;
import mocket.runtime.testbed.FaultController;
import mocket.runtime.testbed.StateMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Mocket {

    final Logger logger = LoggerFactory.getLogger(mocket.Mocket.class);
    /**
     * Configuration settings for Mocket
     */
    Configuration cfg;
    public static int nodeNum = 0;
    boolean initilized = false;

    /**
     * Components of Mocket
     */
    BehaviorController bc;
    StateMonitor sm;
    Client client;
    FaultController fc;

    PathManager pm;

    Mocket(Configuration cfg) {
        this.cfg = cfg;
        logger.info("Start initialize Mocket...");
        loadInputs();
        init();
        initilized = true;
        start();
    }

    private void loadInputs() {
        Graph input = null;
        switch (cfg.getSut()) {
            case "Raft":
                input = new mocket.path.raft.GraphImpl();
                break;
            case "ZooKeeper":
                input = new mocket.path.zk.GraphImpl();
                break;
            default:
                logger.error("Unknown SUT type:", cfg.getSut(), ". Existing NOW!");
                System.exit(0);
                break;
        }
        ArrayList<Transition> paths = input.readGraph("./outputs/ep.edge", "./outputs/ep.node");
        int totalTests = paths.size();
        pm = new PathManager();
        for(int i = 0; i < totalTests; i++) {
            pm.addPath(i + 1, paths.get(i));
            System.out.println("Path [" + (i + 1) + "]:");
            input.printPath(paths.get(i));
        }
    }

    public void init() {
        HashMap<Integer, String> cluster = new HashMap<>();
        String[] clusterString = cfg.getCluster().split(",");
        for(int i = 0; i < clusterString.length; i++)
            cluster.put(i, clusterString[i]);
        initBehaviorController(cfg.getPort(), cluster);
        initStateMonitor();
        if(!cfg.getFaults().equals("")) {
            initFaultController(cfg.getFaults());
        }
        if(!cfg.getClientRequests().equals("")) {
            initClient(cfg.getClientRequests());
        }
        initilized = true;
    }

    private void initBehaviorController(int port, HashMap<Integer, String> cluster) {
        bc = new BehaviorController(port, cluster, cfg.getSut());
    }

    private void initFaultController(String faultTypes) {
        String[] typeStrings = faultTypes.split(",");
        Set<Fault> faults = new HashSet<Fault>();
        for(String type: typeStrings) {
            try {
                faults.add(Fault.valueOf(type));
            } catch (IllegalArgumentException e) {
                logger.error("Unknown fault type: ", type, "! Skip it.");
            }
        }
        fc = new FaultController(faults);
    }

    private void initClient(String clientRequestsPath) {}

    private void initStateMonitor() {}

    public void start() {
        logger.info("Mocket started! Waiting for SUT cluster communication...");
        while(pm.hasNextPath()) {
            Transition initState = pm.getNextPath();
            bc.setTestingPath(initState);
            logger.info("Begin to testing path:", pm.getCurrentTestingPathId(), "storing the results at", cfg.getResults());
            bc.start();
        }
        stop();
    }

    public void stop() {
        logger.info("Mocket stopped!");
    }

    public boolean isInitilized() {
        return initilized;
    }

    public void checkComponents() {
        //TODO: Test if all components can work correctly, e.g., FaultController
        // Inject a node crash fault.
    }
}
