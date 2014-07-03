package MASS;




import java.io.Serializable;

/**
 *
 * @author Tim Chuang
 */
public class RemoteExchangeRequest implements Serializable
{
    private int destinationGlobalLinearIndex;
    private int originGlobalLinearIndex;
    private int inMessageIndex;
    private Object outMessage;
    
    public RemoteExchangeRequest(int destIndex, int origIndex, int inMsgIndex, Object outMsg)
    {
        destinationGlobalLinearIndex = destIndex;
        originGlobalLinearIndex = origIndex;
        inMessageIndex = inMsgIndex;
        outMessage = outMsg;
    }   
    
    public int getDestinationGlobalLinearIndex() { return destinationGlobalLinearIndex; }
    public int getOriginGlobalLinearIndex() { return originGlobalLinearIndex; }
    public int getInMessageIndex() { return inMessageIndex; }
    public Object getOutMessage() { return outMessage; }
         
}
