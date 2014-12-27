package edu.uw.bothell.css.dsl.MASS;

import java.io.Serializable;

@SuppressWarnings("serial")
public class RemoteExchangeRequest implements Serializable {

    private int destGlobalLinearIndex;
    private int orgGlobalLinearIndex;
    private int inMessageIndex;
    private Object outMessage;

	public RemoteExchangeRequest( int destIndex, int orgIndex, int inMsgIndex, Object outMsg ) {
	
		this.destGlobalLinearIndex = destIndex;
		this.orgGlobalLinearIndex = orgIndex;
		this.inMessageIndex = inMsgIndex;
		this.outMessage = outMsg;
    
	}

	public int getOrgGlobalLinearIndex() {
		return orgGlobalLinearIndex;
	}

	public int getDestGlobalLinearIndex() {
		return destGlobalLinearIndex;
	}

	public int getInMessageIndex() {
		return inMessageIndex;
	}

	public Object getOutMessage() {
		return outMessage;
	}

}