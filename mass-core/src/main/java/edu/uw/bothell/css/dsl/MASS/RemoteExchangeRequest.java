package edu.uw.bothell.css.dsl.MASS;

import java.io.Serializable;

public class RemoteExchangeRequest implements Serializable {
    public RemoteExchangeRequest( int destIndex, int orgIndex, int inMsgIndex,
				  Object outMsg ) {
	this.destGlobalLinearIndex = destIndex;
	this.orgGlobalLinearIndex = orgIndex;
	this.inMessageIndex = inMsgIndex;
	this.outMessage = outMsg;
    }

    public int destGlobalLinearIndex;
    public int orgGlobalLinearIndex;
    public int inMessageIndex;
    public Object outMessage;
}