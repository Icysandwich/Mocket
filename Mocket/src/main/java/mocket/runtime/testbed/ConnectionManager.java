package mocket.runtime.testbed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;
import java.nio.channels.UnresolvedAddressException;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * Refer to ZooKeeper.QuorumCnxManager
 */
public class ConnectionManager {


    final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
    /*
     * Maximum capacity of thread queues
     */
    static final int RECV_CAPACITY = 100;
    static final int SEND_CAPACITY = 100;
    static final int PACKETMAXSIZE = 1024 * 1024;

    public final ArrayBlockingQueue<Message> recvQueue;
    /*
     * Object to synchronize access to recvQueue
     */
    private final Object recvQLock = new Object();


    final ConcurrentHashMap<Integer, ArrayBlockingQueue<ByteBuffer>> queueSendMap;
    final ConcurrentHashMap<Integer, SendWorker> senderWorkerMap;

    /*
     * Shutdown flag
     */
    boolean shutdown = false;


    final int port; // used in SUT
    int sid = -1; // -1 for host
    String host = ""; // Mocket address and port

    public final Listener listener;
    /*
     * Counter to count worker threads
     */
    private AtomicInteger threadCnt = new AtomicInteger(0);

    static public class Message {

        public ByteBuffer buffer;
        public int sid;

        Message(ByteBuffer buffer, int serverId) {
            this.buffer = buffer;
            this.sid = serverId;
        }
    }

    /**
     * Used in Mocket host
     * @param port Mocket port
     */
    public ConnectionManager(int port) {
        this.recvQueue = new ArrayBlockingQueue<Message>(RECV_CAPACITY);
        this.queueSendMap = new ConcurrentHashMap<>();
        this.senderWorkerMap = new ConcurrentHashMap<>();
        this.port = port;

        listener = new Listener();
    }

    /**
     * Used in SUT server
     * @param port SUT server port
     * @param sid SUT server id
     * @param host Mocket host and port
     */
    public ConnectionManager(int port, int sid, String host) {
        this.sid = sid;
        this.recvQueue = new ArrayBlockingQueue<Message>(RECV_CAPACITY);
        this.queueSendMap = new ConcurrentHashMap<>();
        this.senderWorkerMap = new ConcurrentHashMap<>();
        this.port = port;
        this.host = host;

        listener = new Listener();
    }

    /**
     * Processes invoke this message to queue a message to send.
     */
    public void toSend(int sid, ByteBuffer b) {
        ArrayBlockingQueue<ByteBuffer> bq = queueSendMap.get(sid);
        if (bq != null) {
            addToSendQueue(bq, b);
        } else {
            logger.error("No queue for server " + sid);
        }
    }

    private void addToSendQueue(ArrayBlockingQueue<ByteBuffer> queue,
                                ByteBuffer buffer) {
        if (queue.remainingCapacity() == 0) {
            try {
                queue.remove();
            } catch (NoSuchElementException ne) {
                // element could be removed by poll()
                logger.debug("Trying to remove from an empty " +
                        "Queue. Ignoring exception " + ne);
            }
        }
        try {
            queue.add(buffer);
        } catch (IllegalStateException ie) {
            // This should never happen
            logger.error("Unable to insert an element in the queue " + ie);
        }
    }

    public boolean receiveConnection(Socket sock) {
        int sid = -1;

        try {
            // Read server id
            DataInputStream din = new DataInputStream(sock.getInputStream());
            sid = din.readInt();
        } catch (IOException e) {
            closeSocket(sock);
            logger.warn("Exception reading or writing challenge: " + e.toString());
            return false;
        }

            SendWorker sw = new SendWorker(sock, sid);
            RecvWorker rw = new RecvWorker(sock, sid, sw);
            sw.setRecv(rw);

            SendWorker vsw = senderWorkerMap.get(sid);

            if(vsw != null)
                vsw.finish();

            senderWorkerMap.put(sid, sw);

            if (!queueSendMap.containsKey(sid)) {
                queueSendMap.put(sid, new ArrayBlockingQueue<ByteBuffer>(
                        SEND_CAPACITY));
            }

            sw.start();
            rw.start();

            return true;
    }

    /**
     * SUT server tries to establish a connection to Mocket server.
     */
    public synchronized void connect(){
        if (senderWorkerMap.get(sid) == null){
            try {
                InetSocketAddress electionAddr = new InetSocketAddress(
                        this.host.substring(0, this.host.indexOf(":")),
                        Integer.parseInt(this.host.substring(this.host.indexOf(":") + 1)));
                if (logger.isDebugEnabled()) {
                    logger.debug("Opening channel to Mocket server on", this.host);
                }
                Socket sock = new Socket();
                sock.connect(electionAddr);
                if (logger.isDebugEnabled()) {
                    logger.debug("Connected to Mocket server on", this.host);
                }
                initiateConnection(sock);
            } catch (UnresolvedAddressException e) {
                logger.warn("Cannot open channel to " + sid
                        + " at address " + host, e);
                throw e;
            } catch (IOException e) {
                logger.warn("Cannot open channel to " + sid
                                + " at address " + host,
                        e);
            }
        } else {
            logger.debug("There is a connection already for server " + sid);
        }
    }

    /**
     * SUT server initiate connection.
     */
    public boolean initiateConnection(Socket sock) {
        DataOutputStream dout = null;
        try {
            // Sending id
            dout = new DataOutputStream(sock.getOutputStream());
            dout.writeInt(sid);
            dout.flush();
        } catch (IOException e) {
            logger.warn("Ignoring exception reading: ", e);
            closeSocket(sock);
            return false;
        }

        SendWorker sw = new SendWorker(sock, sid);
        RecvWorker rw = new RecvWorker(sock, sid, sw);
        sw.setRecv(rw);

        SendWorker vsw = senderWorkerMap.get(sid);

        if(vsw != null)
            vsw.finish();

        senderWorkerMap.put(sid, sw);
        if (!queueSendMap.containsKey(sid)) {
            queueSendMap.put(sid, new ArrayBlockingQueue<ByteBuffer>(
                    SEND_CAPACITY));
        }

        sw.start();
        rw.start();

        return true;
    }



    /**
     * Helper method to close a socket.
     *
     * @param sock
     *            Reference to socket
     */
    private void closeSocket(Socket sock) {
        try {
            sock.close();
        } catch (IOException ie) {
            logger.error("Exception while closing", ie);
        }
    }

    /**
     * Thread to listen on some port
     */
    public class Listener extends Thread {

        volatile ServerSocket ss = null;

        /**
         * Sleeps on accept().
         */
        @Override
        public void run() {
            int numRetries = 0;
            while((!shutdown) && (numRetries < 3)){
                try {
                    ss = new ServerSocket();
                    ss.setReuseAddress(true);
                    InetSocketAddress addr = new InetSocketAddress(port);
                    logger.info("The connection manager listening port: " + addr.toString());
                    setName("Mocket CM");
                    ss.bind(addr);
                    while (!shutdown) {
                        Socket client = ss.accept();
                        logger.info("Received connection request "
                                + client.getRemoteSocketAddress());
                        receiveConnection(client);
                        numRetries = 0;
                    }
                } catch (IOException e) {
                    logger.error("Exception while listening", e);
                    numRetries++;
                    try {
                        ss.close();
                        Thread.sleep(1000);
                    } catch (IOException ie) {
                        logger.error("Error closing server socket", ie);
                    } catch (InterruptedException ie) {
                        logger.error("Interrupted while sleeping. " +
                                "Ignoring exception", ie);
                    }
                }
            }
            logger.info("Leaving listener");
            if (!shutdown) {
                logger.error("I'm leaving the listener thread");
            }
        }

        /**
         * Halts this listener thread.
         */
        void halt(){
            try{
                logger.debug("Trying to close listener: " + ss);
                if(ss != null) {
                    logger.debug("Closing listener.");
                    ss.close();
                }
            } catch (IOException e){
                logger.warn("Exception when shutting down listener: " + e);
            }
        }
    }
    
    /**
     * Thread to send messages. Instance waits on a queue, and send a message as
     * soon as there is one available. If connection breaks, then opens a new
     * one.
     */
    class SendWorker extends Thread {
        int sid;
        Socket sock;
        RecvWorker recvWorker;
        volatile boolean running = true;
        DataOutputStream dout;

        /**
         * An instance of this thread receives messages to send
         * through a queue and sends them to the server sid.
         *
         * @param sock
         *            Socket to remote peer
         * @param sid
         *            Server identifier of remote peer
         */
        SendWorker(Socket sock, int sid) {
            super("SendWorker:" + sid);
            this.sid = sid;
            this.sock = sock;
            recvWorker = null;
            try {
                dout = new DataOutputStream(sock.getOutputStream());
            } catch (IOException e) {
                logger.error("Unable to access socket output stream", e);
                closeSocket(sock);
                running = false;
            }
            logger.debug("Address of remote peer: " + this.sid);
        }

        synchronized void setRecv(RecvWorker recvWorker) {
            this.recvWorker = recvWorker;
        }

        /**
         * Returns RecvWorker that pairs up with this SendWorker.
         *
         * @return RecvWorker
         */
        synchronized RecvWorker getRecvWorker(){
            return recvWorker;
        }

        synchronized boolean finish() {
            if (logger.isDebugEnabled()) {
                logger.debug("Calling finish for " + sid);
            }

            if(!running){
                /*
                 * Avoids running finish() twice.
                 */
                return running;
            }

            running = false;
            closeSocket(sock);
            // channel = null;

            this.interrupt();
            if (recvWorker != null) {
                recvWorker.finish();
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Removing entry from senderWorkerMap sid=" + sid);
            }
            senderWorkerMap.remove(sid, this);
            threadCnt.decrementAndGet();
            return running;
        }

        synchronized void send(ByteBuffer b) throws IOException {
            byte[] msgBytes = new byte[b.capacity()];
            try {
                b.position(0);
                b.get(msgBytes);
            } catch (BufferUnderflowException be) {
                logger.error("BufferUnderflowException ", be);
                return;
            }
            dout.writeInt(b.capacity());
            dout.write(b.array());
            dout.flush();
        }

        @Override
        public void run() {
            threadCnt.incrementAndGet();


            try {
                while (running && !shutdown && sock != null) {

                    ByteBuffer b;
                    try {
                        ArrayBlockingQueue<ByteBuffer> bq = queueSendMap
                                .get(sid);
                        if (bq != null) {
                            b = pollSendQueue(bq, 1000, TimeUnit.MILLISECONDS);
                        } else {
                            logger.error("No queue of incoming messages for " +
                                    "server " + sid);
                            break;
                        }

                        if(b != null){
                            send(b);
                        }
                    } catch (InterruptedException | SocketException e) {
                        logger.debug("Exiting SendWorker for id [{}]", sid);
                    }
                }
            } catch (IOException e) {
                logger.warn("Exception when using channel: for id " + sid, " error = " + e);
            }
            this.finish();
            logger.warn("Send worker leaving thread");
        }
    }

    /**
     * Thread to receive messages. Instance waits on a socket read. If the
     * channel breaks, then removes itself from the pool of receivers.
     */
    class RecvWorker extends Thread {
        int sid;
        Socket sock;
        volatile boolean running = true;
        DataInputStream din;
        final SendWorker sw;

        RecvWorker(Socket sock, int sid, SendWorker sw) {
            super("RecvWorker:" + sid);
            this.sid = sid;
            this.sock = sock;
            this.sw = sw;
            try {
                din = new DataInputStream(sock.getInputStream());
                // OK to wait until socket disconnects while reading.
                sock.setSoTimeout(0);
            } catch (IOException e) {
                logger.error("Error while accessing socket for " + sid, e);
                closeSocket(sock);
                running = false;
            }
        }

        /**
         * Shuts down this worker
         *
         * @return boolean  Value of variable running
         */
        synchronized boolean finish() {
            if(!running){
                /*
                 * Avoids running finish() twice.
                 */
                return running;
            }
            running = false;

            this.interrupt();
            threadCnt.decrementAndGet();
            return running;
        }

        @Override
        public void run() {
            threadCnt.incrementAndGet();
            try {
                while (running && !shutdown && sock != null) {
                    /**
                     * Reads the first int to determine the length of the
                     * message
                     */
                    int length = din.readInt();
                    if (length <= 0 || length > PACKETMAXSIZE) {
                        throw new IOException(
                                "Received packet with invalid packet: "
                                        + length);
                    }
                    /**
                     * Allocates a new ByteBuffer to receive the message
                     */
                    byte[] msgArray = new byte[length];
                    din.readFully(msgArray, 0, length);
                    ByteBuffer message = ByteBuffer.wrap(msgArray);
                    addToRecvQueue(new Message(message.duplicate(), sid));
                }
            } catch (SocketException e) {
                logger.debug("Exiting SenderWorker for id [{}]", sid);
            } catch (EOFException e) {
                logger.debug("Received wrong server packet ", e);
            } catch (IOException e) {
                logger.warn("Connection broken for id " + sid + ", error = " + e);
            } finally {
                logger.warn("Interrupting SendWorker");
                sw.finish();
                if (sock != null) {
                    closeSocket(sock);
                }
            }
        }
    }
    /**
     * Retrieves and removes buffer at the head of this queue,
     * waiting up to the specified wait time if necessary for an element to
     * become available.
     *
     * {@link ArrayBlockingQueue#poll(long, java.util.concurrent.TimeUnit)}
     */
    private ByteBuffer pollSendQueue(ArrayBlockingQueue<ByteBuffer> queue,
                                     long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }
    /**
     * Inserts an element in the {@link #recvQueue}. If the Queue is full, this
     * methods removes an element from the head of the Queue and then inserts
     * the element at the tail of the queue.
     *
     * This method is synchronized to achieve fairness between two threads that
     * are trying to insert an element in the queue. Each thread checks if the
     * queue is full, then removes the element at the head of the queue, and
     * then inserts an element at the tail. This three-step process is done to
     * prevent a thread from blocking while inserting an element in the queue.
     * If we do not synchronize the call to this method, then a thread can grab
     * a slot in the queue created by the second thread. This can cause the call
     * to insert by the second thread to fail.
     * Note that synchronizing this method does not block another thread
     * from polling the queue since that synchronization is provided by the
     * queue itself.
     *
     * @param msg
     *          Reference to the message to be inserted in the queue
     */
    public void addToRecvQueue(Message msg) {
        synchronized(recvQLock) {
            if (recvQueue.remainingCapacity() == 0) {
                try {
                    recvQueue.remove();
                } catch (NoSuchElementException ne) {
                    // element could be removed by poll()
                    logger.debug("Trying to remove from an empty " +
                            "recvQueue. Ignoring exception " + ne);
                }
            }
            try {
                recvQueue.add(msg);
            } catch (IllegalStateException ie) {
                // This should never happen
                logger.error("Unable to insert element in the recvQueue " + ie);
            }
        }
    }

    /**
     * Retrieves and removes a message at the head of this queue,
     * waiting up to the specified wait time if necessary for an element to
     * become available.
     *
     * {@link ArrayBlockingQueue#poll(long, java.util.concurrent.TimeUnit)}
     */
    public Message pollRecvQueue(long timeout, TimeUnit unit)
            throws InterruptedException {
        return recvQueue.poll(timeout, unit);
    }

    /**
     * Flag that it is time to wrap up all activities and interrupt the listener.
     */
    public void halt() {
        shutdown = true;
        logger.debug("Halting listener");
        listener.halt();

        softHalt();
    }

    /**
     * A soft halt simply finishes workers.
     */
    public void softHalt() {
        for (SendWorker sw : senderWorkerMap.values()) {
            logger.info("Halting sender: " + sw);
            sw.finish();
        }
    }

    public void reset() {
        logger.info("Reset all connections");
        softHalt();

        recvQueue.clear();
        queueSendMap.clear();
        senderWorkerMap.clear();
    }
}
