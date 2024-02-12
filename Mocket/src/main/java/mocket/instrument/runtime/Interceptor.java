package mocket.instrument.runtime;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import mocket.path.ActionType;
import mocket.runtime.testbed.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used in target system.
 */
public class Interceptor {

    static final Logger logger = LoggerFactory.getLogger(Interceptor.class);
    final static int blockingCheckInterval = 1000;

    public static String SUT = "";

    /**
     * Different server should connect different port on the host.
     */
    public static String hostAddress = "127.0.0.1:9090";

    public static String myHost;
    public static int sid;
    public static int port = 19090 + sid;


    private static int actionCount = 0;
    private static HashMap<Integer, Boolean> semaphore = new HashMap<>();
    private static HashMap<Integer, String[]> params = new HashMap<>();

    public static LocalState state;

    static ConnectionManager cm;

    static WorkerReceiver wr;

    private static void initLocalState() {
        logger.info("Initiate local state on server[{}]", sid);
        switch(SUT) {
            case "Raft":
                state = new mocket.instrument.runtime.raft.LocalState(null);
                break;
            case "ZooKeeper":
            default:
                logger.error("Unknown system under testing. Exit.");
                System.exit(0);
        }
    }

    private static void initConnectionManager() {
        logger.info("Initiate intercetor on server[{}]", sid);
        cm = new ConnectionManager(port, sid, hostAddress);
        cm.connect();

        wr = new WorkerReceiver(cm);
        Thread t = new Thread(wr, "Interceptor[myid=" + sid + "]");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Receiving response from Mocket host to resume blocked action
     */
    private static class WorkerReceiver implements Runnable{
        volatile boolean stop;
        ConnectionManager cm;

        WorkerReceiver(ConnectionManager cm) {
            this.stop = false;
            this.cm = cm;
        }

        public void run() {
            while(!stop) {
                try {
                    ConnectionManager.Message m = cm.pollRecvQueue(3000, TimeUnit.MILLISECONDS);
                    if(m == null || m.buffer.capacity() < 4 ) continue;
                    if(m.sid != Interceptor.sid) {
                        logger.error("Receive a wrong response for server[{}] on server[{}]!", m.sid, Interceptor.sid);
                        stop();
                    } else {
                        int bid = m.buffer.getInt(); // Blocking id
                        logger.info("Receive host's response for action[{}]", bid);
                        setSemaphore(bid, false);
                    }
                } catch (InterruptedException e) {
                    logger.info("Interrupted Exception while waiting for new message" + e.toString());
                }
            }
        }
    }

    private synchronized static int blockCounter() {
        return actionCount++;
    }

    private synchronized static void setSemaphore(int actionId, boolean isBlocking) {
        semaphore.put(actionId, isBlocking);
    }

    private synchronized static boolean isBlocking(int actionId) {
        if(!semaphore.keySet().contains(actionId))
            return false;
        return semaphore.get(actionId);
    }

    private synchronized static void removeSemaphore(int actionId) {
        semaphore.remove(actionId);
        params.remove(actionId);
    }

    private static void stop() {
        wr.stop = true;
        cm.halt();
    }

    public synchronized static int collectParams(String... params){
        int actionId = blockCounter();
        Interceptor.params.put(actionId, params);
        setSemaphore(actionId, false);
        return actionId;
    }

    /**
     * Used for instrumentation
     * @param action
     * @param actionId
     * @return
     */
    public static boolean notifyAndBlock(String action, int actionId) {
        if (cm == null) {
            initConnectionManager();
        }
        if (state == null) {
            initLocalState();
        }
        if (!semaphore.keySet().contains(actionId)) {
            logger.error("The action [{}] is not correctly set!", action);
            return false;
        }

        logger.info("Process action: {}, id={}", action, actionId);
        byte messageBytes[] = new byte[1024];
        ByteBuffer messageBuffer = ByteBuffer.wrap(messageBytes);
        messageBuffer.clear();
        messageBuffer.putInt(0); // Action control

        messageBuffer.putInt(ActionType.getActionType(action).getValue()); // Action type

        messageBuffer.putInt(actionId); // Blocking id

        String[] param = params.get(actionId);
        messageBuffer.putInt(param.length); // Parameter size
        String allParameters = "";
        for (int i = 0; i < param.length; i++) {
            byte[] paramBytes = param[i].getBytes();
            int len = paramBytes.length;
            messageBuffer.putInt(len);
            messageBuffer.put(paramBytes);
            allParameters = allParameters + param[i] + ",";
        }
        if (allParameters.length() > 0) {
            allParameters = allParameters.substring(0, allParameters.length()-1);
        }
        logger.debug("Action schedule request builded: {}({})", action, allParameters);
        cm.toSend(sid, messageBuffer);
        setSemaphore(actionId, true);
        logger.debug("Sending action schedule request to Mocket server.");
        while (true) { // Block here. Wait for ActionScheduler responses.
            if (!isBlocking(actionId))
                break;
            try {
                Thread.sleep(blockingCheckInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.info("Received Mocket server response for action scheduling. Resumed.");
        removeSemaphore(actionId);
        return true;
    }

    /**
     * Used for instrumentation
     * @param action
     * @param actionId
     * @return
     */
    public static boolean checkState(String action, int actionId) {
        if (cm == null) {
            initConnectionManager();
        }
        if (state == null) {
            initLocalState();
        }
        logger.info("Check state for action: {}, id={}", action, actionId);

        byte messageBytes[] = new byte[1024];
        ByteBuffer messageBuffer = ByteBuffer.wrap(messageBytes);
        messageBuffer.clear();

        messageBuffer.putInt(1); // Variable checking
        messageBuffer.putInt(ActionType.getActionType(action).getValue()); // Action type

        messageBuffer.putInt(actionId); // Action id

        messageBuffer.putInt(state.stateNames().size()); // States
        String stateString = "";
        for(String stateName: state.stateNames()) {
            String stateDetail = stateName + "," + state.getStateValue(stateName);
            byte[] bytes = stateDetail.getBytes();
            int len = bytes.length;
            messageBuffer.putInt(len);
            messageBuffer.put(bytes);
            stateString = stateString + "[" + stateDetail + "]";
        }
        logger.debug("State check request builded for action:{}. The state values:({})", action, stateString);
        cm.toSend(sid, messageBuffer);
        return true;
    }

    /**
     * The node has encountered the testing scenario. We block the current thread
     * to wait for Mocket server checking initial state on all nodes.
     * @return
     */
    public static boolean checkInitState() {
        int actionId = -1;
        if (cm == null) {
            initConnectionManager();
        }
        if (state == null) {
            initLocalState();
        }
        byte messageBytes[] = new byte[1024];
        ByteBuffer messageBuffer = ByteBuffer.wrap(messageBytes);
        messageBuffer.clear();

        messageBuffer.putInt(-1); // Checking initial state
        messageBuffer.putInt(ActionType.NULL.getValue()); // Root action

        messageBuffer.putInt(actionId);

        messageBuffer.putInt(0); // States

        // We do not check the state value details in this version

        // messageBuffer.putInt(states.stateNames().size()); // States
        // for(String stateName: states.stateNames()) {
        //     String state = stateName + "," + states.getStateValue(stateName);
        //     byte[] bytes = state.getBytes();
        //     int len = bytes.length;
        //     messageBuffer.putInt(len);
        //     messageBuffer.put(bytes);
        // }
        setSemaphore(actionId, true);
        logger.info("Sending initial state checking request to Mocket server.");
        cm.toSend(sid, messageBuffer);
        while (true) { // Block here. Wait for ActionScheduler responses.
            try {
                if (!isBlocking(actionId)) {
                    break;
                }
                Thread.sleep(blockingCheckInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.info("Received Mocket server response for initial state checking. Resumed.");
        removeSemaphore(actionId);
        return true;
    }

    public static void startAction(String actionName) {
        // Do nothing. Only for annotation
    }

    public static void endAction(String actionName) {
        // Do nothing. Only for annotation
    }


}
