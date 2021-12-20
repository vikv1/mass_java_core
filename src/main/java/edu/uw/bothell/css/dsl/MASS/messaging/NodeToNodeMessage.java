package edu.uw.bothell.css.dsl.MASS.messaging;

import java.io.Serializable;

// NodeToNodeMessage extends from MASSMessage, adding message type and sourceAddress,
// and sourceID fields.
public class NodeToNodeMessage< T extends Serializable > extends MASSMessage<T> {
    public static enum MessageType {
        REQUEST,
        RESPONSE
    }

    private MessageType messageType;
    private int sourceAddress;
    private int sourceMsgID;

    /**
     * Constructs a new NodeToNodeMessage for use with sending node message requests.
     * 
     * @param destinationAddress The node address for which this message should be sent.
     * @param message The message to send to the node.
     * @param sourceAddress The source node address of this message.
     * @param messageType The type of message being sent (request/response)
     */
    public NodeToNodeMessage(int destinationAddress, T message, int sourceAddress, MessageType messageType) {
        super(destinationAddress, message);

        this.sourceAddress = sourceAddress;
        this.messageType = messageType;
    }

    /**
     * Constructs a new NodeToNodeMessage for use with sending node response messages.
     * 
     * @param destinationAddress The node address to send this response message to.
     * @param message The response message to send.
     * @param sourceAddress The source node address of this message.
     * @param sourceMsgID The source message ID associated with this response message.
     * @param messageType The message type (request/response)
     */
    public NodeToNodeMessage(int destinationAddress, T message, int sourceAddress, int sourceMsgID, MessageType messageType) {
        super(destinationAddress, message);

        this.sourceAddress = sourceAddress;
        this.sourceMsgID = sourceMsgID;
        this.messageType = messageType;
    }

    // Getter/Setters for message attributes.
    public MessageType getMessageType() { return this.messageType; }
    public int getSourceAddress() { return this.sourceAddress; }
    public int getSourceID() { return this.sourceMsgID; }
    public void setSourceID(int sourceID) { this.sourceMsgID = sourceID; }
}
