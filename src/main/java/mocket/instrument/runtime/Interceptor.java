package mocket.instrument.runtime;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import mocket.path.BehaviorType;
import mocket.runtime.States;
import mocket.runtime.testbed.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used in target system.
 */
public class Interceptor {

    static final Logger logger = LoggerFactory.getLogger(Interceptor.class);
    final static int blockingCheckInterval = 1000;


    /**
     * Different server should connect different port on the host.
     */
    public static String hostAddress = "127.0.0.1:9090";
    public static int sid;
    public static int port = 19090 + sid;


    private static int behaviorCount = 0;
    private static HashMap<Integer, Boolean> semaphore = new HashMap<>();
    private static HashMap<Integer, HashMap<String, String>> params = new HashMap<>();

    public static States states;
    static ConnectionManager cm;

    static WorkerReceiver wr;

    static{
        states = new States(null, null);
        states.initStates();
        cm = new ConnectionManager(port, sid, hostAddress);
        cm.connect();

        wr = new WorkerReceiver(cm);
        Thread t = new Thread(wr, "Interceptor[myid=" + sid + "]");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Receiving response from Mocket host to resume blocked behavior
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
                        logger.error("Receive other server's response!");
                        stop();
                    } else {
                        int bid = m.buffer.getInt(); // Blocking id
                        setSemaphore(bid, false);

                    }
                } catch (InterruptedException e) {
                    logger.info("Interrupted Exception while waiting for new message" + e.toString());
                }
            }
        }
    }

    private synchronized static int blockCounter() {
        return behaviorCount++;
    }

    private synchronized static void setSemaphore(int behaviorId, boolean isBlocking) {
        semaphore.put(behaviorId, isBlocking);
    }

    private synchronized static boolean isBlocking(int behaviorId) {
        if(!semaphore.keySet().contains(behaviorId))
            return false;
        return semaphore.get(behaviorId);
    }

    private synchronized static void removeSemaphore(int behaviorId) {
        semaphore.remove(behaviorId);
        params.remove(behaviorId);
    }

    public synchronized static int collectParams(String... params){
        int len = params.length;
        if(len % 2 != 0) {
            logger.error("Incorrect param collection!");
            return -1;
        }
        int behaviorId = blockCounter();
        String paramName = "";
        HashMap<String, String> param = new HashMap<>();
        for(int i = 0; i < len; i++) {
            if(i % 2 == 0)
                paramName = params[i];
            else
                param.put(paramName, params[i]);
        }
        Interceptor.params.put(behaviorId, param);
        return behaviorId;
    }

    /**
     * Used for instrumentation
     * @param behavior
     * @param behaviorId
     * @return
     */
    public static boolean notifyAndBlock(String behavior, int behaviorId) {
        if (cm == null) {
            logger.error("Connection to Mocket server is not initialized correctly!");
            return false;
        }
        if (!semaphore.keySet().contains(behaviorId)) {
            logger.error("Not properly set ");
            return false;
        }
        byte messageBytes[] = new byte[1024];
        ByteBuffer messageBuffer = ByteBuffer.wrap(messageBytes);
        messageBuffer.clear();
        messageBuffer.putInt(0); // Behavior control

        messageBuffer.putInt(BehaviorType.valueOf(behavior).getValue()); // Behavior type

        messageBuffer.putInt(behaviorId); // Blocking id
        setSemaphore(behaviorId, true);

        HashMap<String, String> param = params.get(behaviorId);
        if (!params.get(behaviorId).isEmpty()) {
            messageBuffer.putInt(param.size()); // Parameter number
            for (String paramName : param.keySet()) {
                byte[] paramBytes = (paramName + "," + param.get(paramName)).getBytes();
                int len = paramBytes.length;
                messageBuffer.putInt(len);
                messageBuffer.put(paramBytes);
            }
        }
        cm.toSend(sid, messageBuffer);
        Thread t = new Thread();
        while (true) { // Block here. Wait for BehaviorController responses.
            if (!isBlocking(behaviorId))
                break;
            try {
                t.sleep(blockingCheckInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        removeSemaphore(behaviorId);
        return true;
    }

    /**
     * Used for instrumentation
     * @param behavior
     * @param behaviorId
     * @return
     */
    public static boolean checkAllStates(String behavior, int behaviorId) {
        if(cm == null) {
            logger.error("Connection to Mocket server is not initialized correctly!");
            return false;
        }
        byte messageBytes[] = new byte[1024];
        ByteBuffer messageBuffer = ByteBuffer.wrap(messageBytes);
        messageBuffer.clear();

        messageBuffer.putInt(1); // State checking
        messageBuffer.putInt(BehaviorType.valueOf(behavior).getValue()); // Behavior type

        messageBuffer.putInt(behaviorId); // Behavior id

        messageBuffer.putInt(states.stateNames().size()); // States
        for(String stateName: states.stateNames()) {
            String state = stateName + "," + states.getStateValue(stateName);
            messageBuffer.put(state.getBytes());
        }
        return true;
    }

    private static void stop() {
        wr.stop = true;
        cm.halt();
    }
}
