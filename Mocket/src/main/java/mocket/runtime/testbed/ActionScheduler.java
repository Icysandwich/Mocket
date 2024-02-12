package mocket.runtime.testbed;

import mocket.Mocket;
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
    Timer stateTimer;
    boolean isActionMissing = false;
    boolean isStateCheckingMissing = false;

    private class Request {
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
        this.stateTimer = new Timer();
        this.actionTimeout = timeout;
    }

    public void start() {
        ConnectionManager.Listener listener = cm.listener;
        if (listener != null) {
            logger.info("Successfully start action scheduler!");
            listener.start();
        }
    }

    public void setTestingPath(Transition initState) {
        if (currentTransition != null && currentTransition.hasNext()) {
            logger.error("Current :", currentTransition);
            throw new RuntimeException("Cannot interrupt this testing path!");
        }
        if (initState.isInitialState()) {
            this.currentTransition = initState;
        } else {
            logger.error("Cannot set a general state as the initial state:", currentTransition);
            throw new RuntimeException("Cannot set a general state as the initial state!");
        }
    }

    // Tricky method. Remove it in the future version.
    public void skipInitState() {
        if (currentTransition.isInitialState()) {
            this.currentTransition = currentTransition.next;
        }
    }

    public void scheduleActions() throws InconsistencyException {
        logger.info("Start scheduling actions");
        logger.info("Waiting for the initial state checking for all nodes...");
        boolean isInitStateChecked = false;
        boolean[] checkedNodes = new boolean[Mocket.nodeNum];
        while (running) {
            try {
                if (isActionMissing) {
                    logger.error("Mocket find an INCONSISTENCY!");
                    throw new InconsistencyException("", InconsistencyType.missing_action,
                            currentTransition.action.getActionType().getType(), Util.getLocalTime());
                }
                /** Cannot happen in correct instrumentation. */
                if (isStateCheckingMissing) {
                    logger.error("Action {} is already checked, "
                            + "but the following state checking request is not received in {} milliseconds.",
                            currentTransition.action.getActionType().getType(), actionTimeout);
                    return;
                }
                ConnectionManager.Message m = cm.pollRecvQueue(3000, TimeUnit.MILLISECONDS);
                if (m == null)
                    continue;
                int sid = m.sid;
                Request req = new Request();
                req.requestType = m.buffer.getInt();
                req.actionType = ActionType.getActionType(m.buffer.getInt());
                req.actionId = m.buffer.getInt();
                req.itemSize = m.buffer.getInt();
                logger.info("Received a notification: Server:{}, RequestType:{}, ActionType:{}, itemSize:{}.",
                        sid, req.requestType, req.actionType.getType(), req.itemSize);

                if (!waitingLists.keySet().contains(sid))
                    waitingLists.put(sid, new ArrayList<>());

                String[] values = new String[req.itemSize];
                // Read action parameters/state values
                for (int i = 0; i < req.itemSize; i++) {
                    int len = m.buffer.getInt();

                    byte[] bytes = new byte[len];
                    m.buffer.get(bytes);
                    values[i] = new String(bytes);
                }
                req.values = values;
                String itemValues = "";
                for (String itemValue : req.values) {
                    itemValues += "[" + itemValue + "]";
                }
                logger.debug("The item values:{}", itemValues);

                switch (req.requestType) {
                    case -1:
                        /**
                         * Initial state checking request
                         * {@link mocket.instrument.runtime.Interceptor#checkInitState}
                         */
                        logger.info("Receive initial state checking from server[{}].", sid);

                        if (isInitStateChecked) {
                            replyResume(sid, req.actionId);
                            continue;
                        }

                        // TODO: We do not check the value of initial state for now, but only
                        // count the node number. Note that many values of initial state may
                        // not be initialized in SUT yet.
                        checkedNodes[sid - 1] = true;
                        int count = 0;
                        for (int i = 0; i < checkedNodes.length; i++) {
                            if (checkedNodes[i])
                                count++;
                        }
                        logger.info("We have received {}/{} nodes' initial state checking messages.",
                                count, Mocket.nodeNum);
                        if (count == Mocket.nodeNum) {
                            isInitStateChecked = true;
                            logger.info("Received initial state checking messages from all nodes. Now we " +
                                    "resume all blocked nodes and start the testing scenarios.");
                            // A trick to deterministically trigger Raft's first timeout action
                            if (this.sut.equals("Raft")) {
                                currentTransition.checked();
                                stepNext();
                                int firstTimeoutNode = currentTransition.sid;
                                replyResume(firstTimeoutNode, -1);
                                Thread.sleep(5000);
                                for (int i = 1; i <= count; i++) {
                                    if (i != firstTimeoutNode)
                                        replyResume(i, -1);
                                }
                            } else {
                                for (int i = 1; i <= count; i++) {
                                    replyResume(i, -1);
                                }
                                currentTransition.checked();
                                stepNext();
                            }
                        }
                        continue;
                    case 0:
                        /**
                         * Action scheduling request {}
                         */
                        logger.info("Receive action control request from server[{}]", sid);
                        if (!isInitStateChecked) {
                            /**
                             * The SUT has not reached the testing scenario yet, but it may
                             * execute actions which we annotated and check the following state
                             * modification. For these actions, we directly
                             * reply them to continue.
                             */
                            logger.debug("Receive action control request from server[{}]"
                                    + "before initial state checking.", sid);
                            replyResume(sid, req.actionId);
                            continue;
                        }
                        waitingLists.get(sid).add(req);

                        /**
                         * Briefly check if it is the current action
                         */
                        if (currentTransition.isActionExecuted()
                                || currentTransition.action.getSid() != sid
                                || req.actionType != currentTransition.action.getActionType()) {
                            logger.info("Received action {} from server [{}] is not current waiting action {}. Wait for the next.",
                                    req.actionType.getType(), sid, currentTransition.action.getActionType().getType());
                            continue;
                        }
                        Action a = getAction(req, sid);
                        /**
                         * Check the detailed action parameters
                         */
                        if (currentTransition.action.compare(a)) {
                            // If yes, to the next checking step.
                            logger.info("Action matched: {} from server {}. Waiting for state checking request.",
                                    req.actionType.getType(), sid);
                            actionTimer.cancel();
                            replyResume(sid, req.actionId);
                            stepNext();
                        } else {
                            // If not, leave the request in the waiting list and go on.
                            logger.info(
                                    "Received action {} from server [{}] is not current waiting action {}. Wait for the next.",
                                    req.actionType.getType(), sid, currentTransition.action.getActionType().getType());
                            continue;
                        }
                        waitingLists.get(sid).remove(req);
                        break;
                    case 1:
                        /**
                         * State checking request
                         * {@link mocket.instrument.runtime.Interceptor#checkState}
                         */
                        logger.info("Receive state checking request from server[{}] ", sid);

                        if (!isInitStateChecked)
                            continue;

                        if (!currentTransition.isActionExecuted()) {
                            logger.error("Incorrect state checking point from server[{}]", sid);
                            throw new InterruptedException();
                        }
                        State s = getGlobalState(sid, req.values);
                        /**
                         * Check if the state value is consistent
                         */
                        if (currentTransition.state.compareState(s)) {
                            logger.info("State matched from server {}. Step next.", sid);
                            stateTimer.cancel();
                            stepNext();
                        } else {
                            logger.error("Mocket find an INCONSISTENCY!");

                            throw new InconsistencyException("Spec State:" + currentTransition.state.toString() + 
                                "; Impl State:" + s.toString(), InconsistencyType.incorrect_state,
                                    s.getStateId(), Util.getLocalTime());
                        }
                        break;
                    default:
                        logger.error("Unknown request type!");
                        throw new InterruptedException();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                stop();
                break;
            }
        }
    }

    private Action getAction(Request req, int sid) {
        if (req.requestType != 0)
            return null;
        Action ret = null;
        switch (sut) {
            case "Raft":
                ret = new mocket.path.raft.ActionImpl(req.actionType, req.values);
                break;
            case "ZooKeeper":
            default:
                break;
        }
        return ret;
    }

    /**
     * We received a {@link mocket.instrument.LocalState}. Now transform it
     * into a new global state from the current state.
     * @param sid
     * @param stateValues
     * @return New global state in the implementation
     */
    private State getGlobalState(int sid, String[] stateValues) {
        State prevState;
        if (currentTransition.prev != null)
            prevState = currentTransition.prev.state;
        else {
            if (sut.equals("ZooKeeper"))
                prevState = new mocket.path.zk.State();
            else if (sut.equals("Raft"))
                prevState = new mocket.path.raft.State();
            else {
                // Cannot hanppen
                logger.error("Unknown system under testing. Exit.");
                prevState = null;
            }
        }
        State newState = prevState.updateState(sid, stateValues);
        return newState;
    }

    private void stepNext() throws InconsistencyException {
        if (currentTransition.isStateChecked()) {
            if (currentTransition.hasNext()) {
                currentTransition = currentTransition.next;
            } else {
                // No more actions. Now check if there exist unexpected actions.
                if (!waitingLists.isEmpty()) {
                    Collection<ArrayList<Request>> reqArrays = waitingLists.values();
                    ArrayList<Request> allReqs = new ArrayList<>();
                    // Although in some cases there can exist many unexpected actions,
                    // we only report the first one.
                    for (ArrayList<Request> reqs : reqArrays) {
                        allReqs.addAll(reqs);
                    }

                    logger.error("Mocket find an INCONSISTENCY!");
                    throw new InconsistencyException("", InconsistencyType.unexpected_action,
                            allReqs.get(0).actionType.getType(), Util.getLocalTime());
                }
            }
        } else {
            currentTransition.executed();
            waitStateChecking(currentTransition.sid, currentTransition.action.getActionType(), actionTimeout);
            return;
        }
        /**
         * Check if the following action is already waiting.
         */
        int sid = currentTransition.sid;
        Set<Integer> nodes = waitingLists.keySet();
        if (!nodes.contains(sid)) {
            logger.info("Node {} have not registered yet.", sid);
            return;
        }
        ArrayList<Request> waitingList = waitingLists.get(sid);
        logger.info("Check if action {} from server {} is already waiting.",
                currentTransition.action.getActionType().getType(), sid);
        for (Request req : waitingList) {
            if (req.requestType == 0) {
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
         * we use {@link Client} or {@link FaultController} to proactively execute the
         * action.
         */
        Action next = currentTransition.action;
        if (next.isExternalFault()) {
            fc.injectFault(next);
            stepNext();
        } else if (next.isClientRequest()) {
            client.lanuchClientRequest(next);
            stepNext();
        } else {
            // If the following action is a general action, we set a timer to
            // wait for the incoming request. If we do not receive the request before
            // timeout,
            // we find a missing action inconsistency.
            waitAction(currentTransition.sid, next.getActionType(), actionTimeout);
        }
    }

    private void replyResume(int sid, int actionId) {
        byte messageBytes[] = new byte[4];
        ByteBuffer messageBuffer = ByteBuffer.wrap(messageBytes);
        messageBuffer.clear();
        messageBuffer.putInt(actionId);
        logger.info("Reply server[{}] to continue executing.", sid);
        cm.toSend(sid, messageBuffer);
    }

    private void waitAction(int sid, ActionType type, long actionTimeout) {
        logger.info("Wait for action {} from server {} in {} milliseconds.", type, sid, actionTimeout);
        actionTimer.schedule(new ActionTimer(), actionTimeout);
    }

    private void waitStateChecking(int sid, ActionType type, long stateTimeout) {
        logger.info("Wait for state checking for action {} from server {} in {} milliseconds.", type, sid, actionTimeout);
        stateTimer.schedule(new StateTimer(), stateTimeout);
    }

    public void clearCurrentTestingPath() {
        this.currentTransition = null;
        this.isActionMissing = false;
    }

    public void stop() {
        running = false;
        cm.halt();
        logger.info("Successfully stop action scheduler!");
    }

    public void reset() {
        cm.reset();
        clearCurrentTestingPath();
        logger.info("Successfully reset action scheduler!");
    }

    private class ActionTimer extends java.util.TimerTask {

        @Override
        public void run() throws InconsistencyException {
            isActionMissing = true;
        }
    }

    private class StateTimer extends java.util.TimerTask {

        @Override
        public void run() {
            isStateCheckingMissing = true;
        }
    }
}
