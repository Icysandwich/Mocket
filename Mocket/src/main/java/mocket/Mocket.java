package mocket;

import mocket.faults.Fault;
import mocket.path.Graph;
import mocket.path.GraphException;
import mocket.path.Transition;
import mocket.runtime.testbed.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Mocket {

    final Logger logger = LoggerFactory.getLogger(mocket.Mocket.class);
    /**
     * Configuration settings for Mocket
     */
    // Settings for testing
    String workspace;
    String SUT;
    String inputs;
    String cluster;
    int port;
    long timeout;

    // Script file paths
    String faults;
    String clientRequests;
    String startCluster;
    String stopCluster;

    String results;

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
        this.SUT = cfg.getSut();
        this.workspace = cfg.getWorkspace();

        this.inputs = cfg.getInputs();
        this.cluster = cfg.getCluster();
        this.port = cfg.getPort();
        this.timeout = cfg.getTimeout();

        this.faults = cfg.getFaults();
        this.clientRequests = cfg.getClientRequests();
        this.startCluster = cfg.getStartCluster();
        this.stopCluster = cfg.getStopCluster();

        this.results = cfg.getResults();

        nodeNum = this.cluster.split(",").length;
    }

    Mocket(String config) {
        Properties properties = new Properties();
        try {
            FileInputStream in = new FileInputStream(config);
            properties.load(in);
            logger.info("Find the configuration file:" + config);
            in.close();
        } catch (FileNotFoundException e) {
            logger.error("Cannot find configuration file: {}", config);
            System.exit(0);
        } catch (IOException e) {
            System.exit(0);
        }
        this.SUT = properties.getProperty("SUT");
        this.workspace = properties.getProperty("workspace");
        this.inputs = properties.getProperty("inputs");
        this.cluster = properties.getProperty("cluster");
        logger.info("Testing " + SUT + " for cluster: "+ cluster);

        this.port = Integer.parseInt(properties.getProperty("port"));
        this.timeout = Long.parseLong(properties.getProperty("timeout"));

        this.faults = properties.getProperty("faults");
        this.clientRequests = properties.getProperty("clientRequests");
        this.startCluster = properties.getProperty("startCluster");
        this.stopCluster = properties.getProperty("stopCluster");

        this.results = properties.getProperty("results");

        nodeNum = this.cluster.split(",").length;
    }

    private boolean loadInputs(String dir) {
        Graph input = null;
        switch (this.SUT) {
            case "Raft":
                input = new mocket.path.raft.GraphImpl();
                break;
            case "ZooKeeper":
                input = new mocket.path.zk.GraphImpl();
                break;
            default:
                logger.error("Unknown SUT type:", this.SUT, ". Exit.");
                System.exit(0);
                break;
        }
        ArrayList<Transition> paths;
        try {
            String edgeFile = "", nodeFile = "";
            File dirPath = new File(dir);
            String[] files = dirPath.list();
            for (String file : files) {
                if (file.endsWith(".edge"))
                    edgeFile = file;
                if (file.endsWith(".node"))
                    nodeFile = file;
            }
            paths = input.readGraph(dir + "/" + edgeFile, dir + "/" + nodeFile);
        } catch (IOException e) {
            logger.error("Read graph failed!");
            e.printStackTrace();
            return false;
        } catch (GraphException e) {
            logger.error("Graph structure format error!");
            e.printStackTrace();
            return false;
        }
        int totalTests = paths.size();
        pm = new PathManager();
        for(int i = 0; i < totalTests; i++) {
            pm.addPath(i + 1, paths.get(i));
            logger.debug("Path [" + (i + 1) + "]:");
            logger.debug(input.printPath(paths.get(i)));
        }
        logger.info("{} paths are read.", totalTests);
        return true;
    }

    public void init() {
        logger.info("Initialize Mocket...");
        if(!loadInputs(this.inputs)) {
            logger.error("Load inputs failed. Exit.");
            initialized = false;
            return;
        }

        HashMap<Integer, String> cluster = new HashMap<>();
        String[] clusterString = this.cluster.split(",");
        for(int i = 0; i < clusterString.length; i++)
            cluster.put(i, clusterString[i]);

        initStateMonitor();
        if(!this.faults.equals("")) {
            initFaultController(faults);
        }
        if(!this.clientRequests.equals("")) {
            initClient(clientRequests);
        }
        initActionScheduler(this.fc, this.client, this.port, cluster, this.timeout);
        logger.info("Successfully initialize Mocket.");
        initialized = true;
    }

    private void initActionScheduler(FaultController faultController,
                                     Client client, int port, HashMap<Integer, String> cluster,
                                     long timeout) {
        as = new ActionScheduler(faultController, client, port, cluster, this.SUT, timeout);
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
        fc.loadFaultScripts();
    }

    private void initClient(String clientRequestsPath) {}

    private void initStateMonitor() {}

    public void start() {
        as.start();
        logger.info("Mocket started! Waiting for SUT cluster communication...");
        while(pm.hasNextPath()) {
            Transition initState = pm.getNextPath();
            as.setTestingPath(initState);
            //as.skipInitState(); // We do not check initial state in this version.
            startCluster();
            try {
                logger.info("Begin testing path:", pm.getCurrentTestingPathId(), "storing the results at", this.results);
                as.scheduleActions();
            } catch (InconsistencyException e) {
                logger.info("Inconsistency: {}", e.getMessage());
                saveReport(this.results, pm.getCurrentTestingPathId(), e);
                as.reset();
                stopCluster();
                as.clearCurrentTestingPath();
                continue;
            }
            logger.info("Finish testing path:", pm.getCurrentTestingPathId());
            as.reset();
            stopCluster();
        }
        logger.info("Finish testing all paths! Exit.");
        stop();
    }

    private void startCluster() {
        Util.invokeScript(this.startCluster, "", workspace);
        logger.info("The SUT cluster is started!");
    }

    private void stopCluster() {
        Util.invokeScript(this.stopCluster, "", workspace);
        logger.info("The SUT cluster is stopped!");
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
                logger.info("The report is saved at: {}", path);
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
