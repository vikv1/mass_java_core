package edu.uw.bothell.css.dsl.MASS.messaging;

import java.io.Serializable;

// MASSReqRespMessage extends from MASSMessage, adding message type and source
// address fields.
public class MASSReqRespMessage< T extends Serializable > extends MASSMessage<T> {
    public static enum MessageType {
        REQUEST,
        RESPONSE
    }

    private MessageType messageType;
    private int sourceAddress;

    public MASSReqRespMessage(int destinationAddress, T message, int sourceAddress, MessageType messageType) {
        super(destinationAddress, message);

        this.sourceAddress = sourceAddress;
        this.messageType = messageType;
    }

    public MessageType getMessageType() { return this.messageType; }
    public int getSourceAddress() { return this.sourceAddress; }
}
