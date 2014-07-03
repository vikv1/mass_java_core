import MASS.*;             // Library for Multi-Agent Spatial Simulation
import java.util.Random;
import java.util.Vector;   // for Vector

public class AgentTest extends Agent {
    
    public AgentTest( ) {
	super( );
    }

    public AgentTest( Object object ) {
	super( );
    }
   
    // function identifiers
    public static final int whoAmI_ = 0;
    public static final int cloneMyself_ = 1;
    
    // this is called from callAll( ) and forwards this call
    // to the appropriate function, based on funcId.
    //------------------------------------------------------
    public Object callMethod( int funcId, Object args ) 
    {
	switch ( funcId ) {
	case whoAmI_: return whoAmI( args );
	case cloneMyself_: return cloneMyself( args );
        }
        return null;
    }

    // 
    //------------------------------------------------------
    public Object whoAmI( Object arg ) {
	int phase = ( ( Integer )arg ).intValue( );
	MASS.log( "phase " + phase + ": thread[" + MASS.getThreadId( ) + 
		  "] called agent(" + agentId + ").whoAmI() at land[" +
		  index[0] + ", " + index[1] + "]");
	
	return null;
    }
    
    // 
    //------------------------------------------------------
    public Object cloneMyself( Object arg ) {
	int nChildren = 1;
	Object[] args = new Object[1];

	this.spawn( 1, args );
	
	return null;
    }

    public static void main( String[] args ) throws Exception {

	String login   = args[0];
	String pass    = args[1];
	String port    = args[2];
	int size       = Integer.parseInt( args[3] );
	int nAgents    = Integer.parseInt( args[4] );
	int nProcesses = Integer.parseInt( args[5] );
	int nThreads   = Integer.parseInt( args[6] );

	String[] massArgs = new String[4];
	massArgs[0] = login;
	massArgs[1] = pass;
	massArgs[2] = "machinefile.txt";
	massArgs[3] = port;

	MASS.init( massArgs, nProcesses, nThreads );

	Places land = new Places( 1, "Land", null, size, size );
	Agents nomad = new Agents( 2, "AgentTest", null, land, nAgents );

	MASS.log( "phase 1.........." );
	nomad.callAll( AgentTest.whoAmI_, ( Object )( new Integer( 1 ) ) );
	nomad.callAll( AgentTest.cloneMyself_ );
	nomad.manageAll( );
	MASS.log( "phase 2.........." );
	nomad.callAll( AgentTest.whoAmI_, ( Object )( new Integer( 2 ) ) );

	MASS.finish( );
    }
}