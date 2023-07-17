package mocket.runtime.testbed;

import mocket.Util;
import mocket.path.Action;
import mocket.path.ActionType;
import mocket.path.State;
import mocket.path.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ActionScheduler {

    final Logger logger = LoggerFactory.getLogger(ActionScheduler.class);
    boolean running;

    int port;
    HashMap<Integer, String> cluster;
    String sut;
    long actionTimeout;

    FaultController fc;
    Client client;
    ConnectionManager cm;

    Timer actionTimer;

    private class Request{
        int requestType;
        ActionType actionType;
        int actionId;
        int itemSize;
        String[] values;
    }

    Transition currentTransition;
    HashMap<Integer, ArrayList<Request>> waitingLists = new HashMap<>();

    public ActionScheduler(FaultController fc, Client client, int listenPort,
                           HashMap<Integer, String> SUTCluster, String SUT, long timeout) {
        this.running = true;
        this.port = listenPort;
        this.cluster = SUTCluster;
        this.sut = SUT;
        this.fc = fc;
        this.client = client;
        this.cm = new ConnectionManager(port);
        this.actionTimer = new Timer();
        this.actionTimeout = timeout;
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

    public void scheduleActions() throws InconsistencyException{
        while(running) {
            try {
                // TODO: now we do not handle with the case that the first action is missing
                ConnectionManager.Message m = cm.pollRecvQueue(3000, TimeUnit.MILLISECONDS);
                if(m == null) continue;
                int sid = m.sid;
                Request req = new Request();
                req.requestType = m.buffer.getInt();
                req.actionType = ActionType.getActionType(m.buffer.getInt());
                req.actionId = m.buffer.getInt();
                req.itemSize = m.buffer.getInt();
                String[] values = new String[req.itemSize];
                // Read action parameters/state values
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
                    // Action control, compare action parameters
                    logger.info("Receive action control request from server: ", sid);
                    /**
                     * Check if it is the current action
                     */
                    if(currentTransition.isActionExecuted()
                            || req.actionType != currentTransition.action.getType()
                            || req.itemSize != currentTransition.action.getParameters().size()) {
                        continue;
                    }
                    Action b = getAction(req, sid);
                    /**
                     * Check the detailed action parameters
                     */
                    if(currentTransition.action.compare(b)) {
                        // If yes, to the next checking step.
                        actionTimer.cancel();
                        replyResume(sid, req.actionId);
                        stepNext();
                    } else {
                        // If not, leave the request in the waiting list and go on.
                        continue;
                    }
                } else if (req.requestType == 1) {
                    // Variable checking, compare state values
                    logger.info("Receive state checking request from server: " + sid);
                    if(!currentTransition.isActionExecuted()) {
                        logger.error("Incorrect state checking point from node" + sid);
                        throw new InterruptedException();
                    }
                    State s = getState(req);
                    /**
                     * Check if the state value is consistent
                     */
                    if(currentTransition.state.compareState(s)) {
                        stepNext();
                    } else {
                        logger.error("Mocket find an INCONSISTENCY!");
                        throw new InconsistencyException("", InconsistencyType.incorrect_state,
                                s.getStateId(), Util.getLocalTime());
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
            }
        }
    }

    private Action getAction(Request req, int sid) {
        if(req.requestType != 0)
            return null;
        Action ret = null;
        if (sut.equals("ZooKeeper")) {
            ret = new mocket.path.zk.ActionImpl(req.actionType, sid);
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

    private void stepNext() {
        if(currentTransition.isStateChecked()) {
            if(currentTransition.hasNext()) {
                currentTransition = currentTransition.next;
            } else {
                // No more actions. Now check if there exist unexpected actions.
                if (!waitingLists.isEmpty()) {
                    Collection<ArrayList<Request>> reqArrays = waitingLists.values();
                    ArrayList<Request> allReqs = new ArrayList<>();
                    // Although in some cases there can exist many unexpected actions,
                    // we only report the first one.
                    for(ArrayList<Request> reqs : reqArrays) {
                        allReqs.addAll(reqs);
                    }

                    logger.error("Mocket find an INCONSISTENCY!");
                    throw new InconsistencyException("", InconsistencyType.unexpected_action,
                            allReqs.get(0).actionType.getType(), Util.getLocalTime());
                }
            }
        } else {
            currentTransition.executeAction();
            return;
        }
        /**
         * Check if the following action is already waiting.
         */
        int sid = currentTransition.sid;
        ArrayList<Request> waitingList = waitingLists.get(sid);
        for(Request req : waitingList) {
            if(req.requestType == 0) {
                if (currentTransition.action.compare(getAction(req, sid))) {
                    actionTimer.cancel();
                    replyResume(sid, req.actionId);
                    stepNext();
                    break;
                }
            }
        }
        /**
         * If the following action is a client request or to-be-injected fault,
         * we use {@link Client} or {@link FaultController} to proactively execute the action.
         */
        Action next = currentTransition.action;
        if(next.isExternalFault()) {
            fc.injectFault(next);
            currentTransition.executeAction();
            stepNext();
        } else if(next.isClientRequest()) {
            client.lanuchClientRequest(next);
            currentTransition.executeAction();
            stepNext();
        } else {
            // If the following action is a general action, we set a timer to
            // wait for the incoming request. If we never receive the request,
            // we find a missing action inconsistency.
            actionTimer.schedule(new ActionTimer(next.getType()), actionTimeout);
        }
    }

    private void replyResume(int sid, int actionId) {
        byte messageBytes[] = new byte[4];
        ByteBuffer messageBuffer = ByteBuffer.wrap(messageBytes);
        messageBuffer.clear();
        messageBuffer.putInt(actionId);
        cm.toSend(sid, messageBuffer);
    }

    public void stop() {
        running = false;
        cm.halt();
    }

    private class ActionTimer extends java.util.TimerTask{
        ActionType actionType;

        public ActionTimer(ActionType type) {
            this.actionType = type;
        }

        @Override
        public void run() {
            throw new InconsistencyException("", InconsistencyType.missing_action,
                    actionType.getType(), Util.getLocalTime());
        }
    }
}
