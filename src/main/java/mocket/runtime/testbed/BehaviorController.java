package mocket.runtime.testbed;

import mocket.path.Behavior;
import mocket.path.BehaviorType;
import mocket.path.State;
import mocket.path.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class BehaviorController {

    final Logger logger = LoggerFactory.getLogger(mocket.runtime.testbed.BehaviorController.class);
    boolean running;

    int port;
    HashMap<Integer, String> cluster;
    String sut;

    ConnectionManager cm;

    private class Request{
        int requestType;
        BehaviorType behaviorType;
        int behaviorId;
        int itemSize;
        String[] values;
    }

    Transition currentTransition;
    HashMap<Integer, ArrayList<Request>> waitingLists = new HashMap<>();

    public BehaviorController(int listenPort, HashMap<Integer, String> SUTCluster, String SUT) {
        this.running = true;
        this.port = listenPort;
        this.cluster = SUTCluster;
        this.sut = SUT;
        this.cm = new ConnectionManager(port);
    }

    public void start() {
        ConnectionManager.Listener listener = cm.listener;
        if(listener != null) {
            listener.start();
        }
    }
    public void setTestingPath(Transition initState) {
        if(currentTransition != null && currentTransition.hasNext()) {
            logger.error("Current :", currentTransition);
            throw new RuntimeException("Cannot interrupt this testing path!");
        }
        if(initState.isInitialState()) {
            this.currentTransition = initState;
        } else{
            logger.error("Cannot set a general state as the initial state:", currentTransition);
            throw new RuntimeException("Cannot set a general state as the initial state!");
        }
    }

    public void checkBehaviors() throws InconsistencyException{
        while(running) {
            try {
                ConnectionManager.Message m = cm.pollRecvQueue(3000, TimeUnit.MILLISECONDS);
                if(m == null) continue;
                int sid = m.sid;
                Request req = new Request();
                req.requestType = m.buffer.getInt();
                req.behaviorType = BehaviorType.getBehaviorType(m.buffer.getInt());
                req.behaviorId = m.buffer.getInt();
                req.itemSize = m.buffer.getInt();
                String[] values = new String[req.itemSize];
                // Read behavior parameters/state values
                for (int i = 0; i < req.itemSize; i ++) {
                    int len = m.buffer.getInt();
                    byte[] bytes = new byte[len];
                    m.buffer.get(bytes);
                    values[i] = new String(bytes);
                }
                req.values = values;
                if(!waitingLists.keySet().contains(sid))
                    waitingLists.put(sid, new ArrayList<>());
                waitingLists.get(sid).add(req);
                if(req.requestType == 0) {
                    // Behavior control, compare behavior parameters
                    logger.info("Receive behavior control request from server: ", sid);
                    /**
                     * Check if it is the current behavior
                     */
                    if(currentTransition.isBehaviorExecuted()
                            || req.behaviorType != currentTransition.behavior.getType()
                            || req.itemSize != currentTransition.behavior.getParameters().size()) {
                        continue;
                    }
                    Behavior b = getBehavior(req, sid);
                    /**
                     * Check the detailed behavior parameters
                     */
                    if(currentTransition.behavior.compare(b)) {
                        // If yes, step down.
                        replyResume(sid, req.behaviorId);
                        stepDown();
                    } else {
                        // If not, just leave the request in the waiting list
                        // and go on.
                        continue;
                    }
                } else if (req.requestType == 1) {
                    // State checking, compare state values
                    logger.info("Receive state checking request from server: " + sid);
                    if(!currentTransition.isBehaviorExecuted()) {
                        logger.error("Incorrect state checking point from node" + sid);
                        throw new InterruptedException();
                    }
                    State s = getState(req);
                    /**
                     * Check if the state value is consistent
                     */
                    if(currentTransition.state.compareState(s)) {
                        stepDown();
                    } else {
                        throw new InconsistencyException("", InconsistencyType.incorrect_state);
                    }
                } else {
                    logger.error("Unknown request type!");
                    throw new InterruptedException();
                }
                waitingLists.get(sid).remove(req);
            } catch (InterruptedException e) {
                e.printStackTrace();
                stop();
                break;
            } catch (InconsistencyException e) {
                logger.info("Mocket find an INCONSISTENCY!");
                stop();
                throw e;
            }
        }
    }

    private Behavior getBehavior(Request req, int sid) {
        if(req.requestType != 0)
            return null;
        Behavior ret = null;
        if (sut.equals("ZooKeeper")) {
            ret = new mocket.path.zk.BehaviorImpl(req.behaviorType, sid);
        } else if (sut.equals("Raft")) {

        }
        return ret;
    }

    private State getState(Request req) {
        if(req.requestType != 1)
            return null;
        State ret = null;
        if (sut.equals("ZooKeeper")) {
            ret = new mocket.path.zk.State();
        } else if (sut.equals("Raft")) {

        }
        return ret;
    }

    private void stepDown() {
        if(currentTransition.isStateChecked()) {
            currentTransition = currentTransition.next;
        } else {
            currentTransition.executeBehavior();
            return;
        }
        /**
         * Check if the following behavior is already waiting. 
         */
        int sid = currentTransition.sid;
        ArrayList<Request> waitingList = waitingLists.get(sid);
        for(Request req : waitingList) {
            if(req.requestType == 0) {
                if (currentTransition.behavior.compare(getBehavior(req, sid))) {
                    replyResume(sid, req.behaviorId);
                    stepDown();
                    break;
                }
            }
        }
    }

    private void replyResume(int sid, int behaviorId) {
        byte messageBytes[] = new byte[4];
        ByteBuffer messageBuffer = ByteBuffer.wrap(messageBytes);
        messageBuffer.clear();
        messageBuffer.putInt(behaviorId);
        cm.toSend(sid, messageBuffer);
    }

    public void stop() {
        running = false;
        cm.halt();
    }

}
