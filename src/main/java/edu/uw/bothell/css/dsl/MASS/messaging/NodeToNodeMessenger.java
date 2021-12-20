package edu.uw.bothell.css.dsl.MASS.messaging;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;

import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.annotations.OnMessage;

/**
 * NodeToNodeMessenger implements logic for sending messages between nodes 
 * in MASS. Specifically request messages that have associated response messages.
 */
public abstract class NodeToNodeMessenger< S extends Serializable > {
    // sendMessageQueue contains messages to be sent and decouples the sending
    // of messages from the processing of them.
    private LinkedBlockingQueue< NodeToNodeMessage< S > > sendMessageQueue = new LinkedBlockingQueue< NodeToNodeMessage< S > >();

    // recvMessageQueue contains received messages and decouples the receiving
    // of messages from the processing of them.
    private LinkedBlockingQueue< NodeToNodeMessage< S > > recvMessageQueue = new LinkedBlockingQueue< NodeToNodeMessage< S > >();

    // callBackFunctions persists stores a KV pair of message ID and a callback function.
    // The callback function is provided by the caller when sending a request. Upon
    // receiving the response, the callback is looked up and executed.
    private ConcurrentHashMap<Integer, Consumer< S > > callBackFunctions = new ConcurrentHashMap<Integer, Consumer< S > >();

    // sendThread processes the sendMessageQueue and is the thread that
    // handles sending messages to nodes.
    private Thread sendThread;

    // recvThread processes the recvMessageQueue and is the thread that 
    // handles receiving message from nodes.
    private Thread recvThread;

    /**
     * Constructs a NodeToNodeMessenger with logic for sending and receiving node
     * messages and responses.
     */
    public NodeToNodeMessenger() {
        // Spin up a request handler to handle request messages.
        this.sendThread = new Thread(() -> this.sendHandler());

        // Spin up a response handler to handle response messages.
        this.recvThread = new Thread(() -> this.recvHandler());

        // Start our send/recv threads.
        this.sendThread.start();
        this.recvThread.start();

        // Register class with node messaging system.
        MASSMessaging.getInstance().registerNodeListener(this);
    }

    /**
     * Close interrupts the send/recv threads, forcing them to return, and
     * shuts down the node-to-node messenger
     */
    public void close() {
        this.sendThread.interrupt();
        this.recvThread.interrupt();
    }

    // sendRequest sends the provided message to the node identified by the
    // provided node ID. If a callback is provided, it is stored in the 
    // callBackFunction map in anticipation of the response value.
    // This method is asynchronous and returns immediately.
    public void sendRequest(int nodeID, S message, Consumer< S > callback) { 
        NodeToNodeMessage<S> msg = new NodeToNodeMessage<S>(
            nodeID,                                 // destination
            message,                                // request message
            MASS.getMyPid(),                        // source
            NodeToNodeMessage.MessageType.REQUEST  // message type
        );

        this.sendMessageQueue.add(msg);
        if (callback != null) {
            this.callBackFunctions.put(Integer.valueOf(msg.getMessageID()), callback);
        }
    }

    /**
     * OnMessage is called by the Aeron messaging system when we receive new messages.
     * Upon receipt, it enqueues them for processing.
     * 
     * @param msg The message received.
     */
    @OnMessage
    public void OnMessage(NodeToNodeMessage< S > msg) {
        this.recvMessageQueue.add(msg);
    }

    /**
     * sendHandler dequeues messages from the send queue and sends them to the
     * appropriate node.
     */
    private void sendHandler() {
        // Dequeue and send messages.
        while( true ) {
            NodeToNodeMessage< S > msg = null;
            try {
                msg = this.sendMessageQueue.take();
            } catch (InterruptedException e) {
                MASS.getLogger().debug("send handler thread interrupted, closing thread.");
                
                return;
            }

            MASSMessaging.getInstance().sendNodeMessage(msg.getDestinationAddress(), msg);
        }
    }

    /**
     * recvHandler dequeues messages from the recv queue and processes them
     * as appropriate. If the return value from the request handler is not
     * null, it is enqueued to the send queue to be returned to the caller.
     */
    private void recvHandler() {
        while( true ) {
            NodeToNodeMessage< S > msg = null;
            try {
                msg = this.recvMessageQueue.take();
            } catch(InterruptedException e) {
                MASS.getLogger().debug("recv handler thread interrupted, closing thread.");
                
                return;
            }

            // switch on message type, handling request separately from responses.
            switch (msg.getMessageType()) {
                // If it's a request, pass it to the request handler. If we get
                // a return value, enqueue a message to return it to the caller.
                case REQUEST:
                    Function< S, S > func = this.getRequestHandler();
                    S returnValue = func.apply(msg.getMessage());

                    // If the function returns a value, enqueue it to be sent back to the caller.
                    if (returnValue != null) {
                        this.sendMessageQueue.add(
                            new NodeToNodeMessage< S >(
                                msg.getSourceAddress(),                 // destination
                                returnValue,                            // return value
                                MASS.getMyPid(),                        // source node
                                msg.getMessageID(),                     // source message ID
                                NodeToNodeMessage.MessageType.RESPONSE  // message type
                            )
                        );
                    }

                    break;

                // If it's a response, we should have a registered callback.
                // Look it up, pass the response to it, and then remove the
                // callback func from the map.
                case RESPONSE:
                    Consumer< S > callback = this.callBackFunctions.get(Integer.valueOf(msg.getSourceID()));
                    if (callback != null) {
                        callback.accept(msg.getMessage());
                        this.callBackFunctions.remove(msg.getMessageID());
                    }

                    break;
            }
        }
    }

    // This is implemented by the subclass and used to handle incoming requests.
    public abstract Function< S, S > getRequestHandler();
}
