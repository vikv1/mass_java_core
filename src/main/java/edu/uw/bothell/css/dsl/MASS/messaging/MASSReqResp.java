package edu.uw.bothell.css.dsl.MASS.messaging;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;

import edu.uw.bothell.css.dsl.MASS.MASS;

public abstract class MASSReqResp {
    private LinkedBlockingQueue< MASSReqRespMessage<? extends Serializable > > sendMessageQueue = new LinkedBlockingQueue< MASSReqRespMessage< ? extends Serializable > >();
    private LinkedBlockingQueue< MASSReqRespMessage< ? extends  Serializable > > recvMessageQueue = new LinkedBlockingQueue< MASSReqRespMessage< ? extends Serializable > >();
    private ConcurrentHashMap<Integer, Consumer<Serializable> > callBackFunctions = new ConcurrentHashMap<Integer, Consumer<Serializable> >();

    public MASSReqResp() {
        // Spin up a request handler to handle request messages.
        new Thread(() -> this.sendHandler());

        // Spin up a response handler to handle response messages.
        new Thread(() -> this.recvHandler());
    }

    // sendRequest sends the provided message to the node identified by the
    // provided node ID. If a callback is provided, it is stored in the 
    // callBackFunction map in anticipation of the response value.
    // This method is asynchronous and returns immediately.
    public <T extends Serializable> void sendRequest(int nodeID, T message, Consumer<Serializable> callback) { 
        MASSReqRespMessage<T> msg = new MASSReqRespMessage<T>(
            nodeID,                                 // destination
            message,                                // request message
            MASS.getMyPid(),                        // source
            MASSReqRespMessage.MessageType.REQUEST  // message type
        );

        this.sendMessageQueue.add(msg);

        if (callback != null) {
            this.callBackFunctions.put(Integer.valueOf(msg.getMessageID()), callback);
        }
    }

    /**
     * sendHandler dequeues messages from the send queue and sends them to the
     * appropriate node.
     */
    private void sendHandler() {
        // Dequeue and send messages.
        while( true ) {
            MASSReqRespMessage< ? extends Serializable > msg = this.recvMessageQueue.poll();
            MASSMessaging.getInstance().sendNodeMessage(msg.getDestinationAddress(), msg);
        }
    }

    /**
     * recvHandler dequeues messages from the recv queue and processes them
     * as appropriate. If the return value from the request handler is not
     * null, it is enqueued to the send queue to be returned to the caller.
     * 
     * @param <R> The return type of the request handler for processing requests.
     */
    private < R extends Serializable > void recvHandler() {
        while( true ) {
            MASSReqRespMessage< ? extends Serializable > msg = this.recvMessageQueue.poll();
            switch (msg.getMessageType()) {
                // If it's a request, pass it to the request handler. If we get
                // a return value, enqueue a message to return it to the caller.
                case REQUEST:
                    Function< Serializable, Serializable > func = this.getRequestHandler();
                    Serializable retVal = func.apply(msg.getMessage());

                    // If the function returns a value, enqueue it to be sent back to the caller.
                    if (retVal != null) {
                        this.sendMessageQueue.add(
                            new MASSReqRespMessage<Serializable>(
                                msg.getSourceAddress(),                     // destination
                                retVal,                                     // return value
                                MASS.getMyPid(),                            // source
                                MASSReqRespMessage.MessageType.RESPONSE     // message type
                            )
                        );
                    }

                    break;

                // If it's a response, we should have a registered callback.
                // Look it up, pass the response to it, and then remove the
                // callback func from the map.
                case RESPONSE:
                    Consumer<Serializable> callback = this.callBackFunctions.get(Integer.valueOf(msg.getMessageID()));
                    callback.accept(msg.getMessage());
                    this.callBackFunctions.remove(msg.getMessageID());

                    break;
            }
        }
    }

    // This is implemented by the subclass and used to handle incoming requests.
    public abstract < T extends Serializable, R extends Serializable > Function< T, R > getRequestHandler();
}
