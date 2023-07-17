package mocket;

import mocket.faults.Fault;
import mocket.path.Graph;
import mocket.path.Transition;
import mocket.runtime.testbed.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class Mocket {

    final Logger logger = LoggerFactory.getLogger(mocket.Mocket.class);
    /**
     * Configuration settings for Mocket
     */
    Configuration cfg;
    public static int nodeNum = 0;
    boolean initialized = false;

    /**
     * Components of Mocket
     */
    ActionScheduler as;
    StateMonitor sm;
    Client client;
    FaultController fc;

    PathManager pm;

    Mocket(Configuration cfg) {
        this.cfg = cfg;
        logger.info("Start initialize Mocket...");
        loadInputs(cfg.getPaths());
        init();
        initialized = true;
        start();
    }

    private void loadInputs(String dir) {
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
        ArrayList<Transition> paths = input.readGraph(dir+"/ep.edge", dir+"/ep.node");
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
        initStateMonitor();
        if(!cfg.getFaults().equals("")) {
            initFaultController(cfg.getFaults());
        }
        if(!cfg.getClientRequests().equals("")) {
            initClient(cfg.getClientRequests());
        }
        initActionScheduler(this.fc, this.client, cfg.getPort(), cluster, cfg.getTimeout());
        initialized = true;
    }

    private void initActionScheduler(FaultController faultController,
                                     Client client, int port, HashMap<Integer, String> cluster,
                                     long timeout) {
        as = new ActionScheduler(faultController, client, port, cluster, cfg.getSut(), timeout);
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
        as.start();
        logger.info("Mocket started! Waiting for SUT cluster communication...");
        while(pm.hasNextPath()) {
            Transition initState = pm.getNextPath();
            as.setTestingPath(initState);
            try {
                logger.info("Begin to testing path:", pm.getCurrentTestingPathId(), "storing the results at", cfg.getResults());
                as.scheduleActions();
            } catch (InconsistencyException e) {
                logger.info(e.getMessage());
                saveReport(cfg.getResults(), pm.getCurrentTestingPathId(), e);
                continue;
            }
        }
        stop();
    }


    private void saveReport(String path, int pathId, InconsistencyException inconsistency) {
        File savePath = new File(path);
        if(savePath.isDirectory()) {
            path += pathId + "_" + inconsistency.getTime() + ".report";
            File reportFile = new File(path);
            if(reportFile.exists()) {
                reportFile.delete();
            }
            try {
                reportFile.createNewFile();
                FileOutputStream fos = new FileOutputStream(reportFile);
                OutputStreamWriter osw = new OutputStreamWriter(fos);
                osw.write(inconsistency.toString());

                osw.close();
                fos.close();
            } catch(IOException e) {
                logger.error("Save inconsistency report failed!" + e.getMessage());
            }
        } else {
            logger.error("Save inconsistency report failed! Path must be the directory!");
        }
    }

    public void stop() {
        as.stop();
        logger.info("Mocket stopped!");
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void checkComponents() {
        //TODO: Test if all components can work correctly, e.g., FaultController
        // Inject a node crash fault.
    }
}
