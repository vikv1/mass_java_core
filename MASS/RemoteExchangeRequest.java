package MASS;

public class RemoteExchangeRequest {
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