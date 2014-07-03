package MASS;


import java.io.Serializable;

/**
 *
 * @author Tim Chuang
 */
public class RemoteExchangeRequest implements Serializable
{
	// Variables
    private Boolean shadowBoundary;
    private int 	destinationGlobalLinearIndex;
    private int 	originGlobalLinearIndex;
    private int 	inMessageIndex;
    private Object 	outMessage;
   
	// Constructors 
    public RemoteExchangeRequest(int destIndex, int origIndex, int inMsgIndex, Object outMsg)
    {
        shadowBoundary                  = false;
        destinationGlobalLinearIndex 	= destIndex;
        originGlobalLinearIndex 		= origIndex;
        inMessageIndex 					= inMsgIndex;
        outMessage 						= outMsg;
    }   

    public RemoteExchangeRequest(  int destIndex, int bndryIndex, Object outMsg )
    {
        shadowBoundary                  = true;
        destinationGlobalLinearIndex    = destIndex;
        originGlobalLinearIndex         = bndryIndex;
        inMessageIndex                  = -1;
        outMessage                      = outMsg;
    }

	// Functions
    public Boolean isBoundaryRqst()              { return shadowBoundary; }
    public int getDestinationGlobalLinearIndex() { return destinationGlobalLinearIndex; }
    public int getOriginGlobalLinearIndex() 	 { return originGlobalLinearIndex; }
    public int getBndryIndex()                   { return originGlobalLinearIndex; }
    public int getInMessageIndex() 				 { return inMessageIndex; }
    public Object getOutMessage() 				 { return outMessage; }
         
}
