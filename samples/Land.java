import MASS.*;

public class Land extends Place {
    public static final int init_ = 0;
    public static final int callalltest_ = 1;
    public static final int exchangetest_ = 2;
    public static final int checkInMessage_ = 3;
    public static final int printOutMessage_ = 4;
    public static final int printShadow_ = 5;

    public Land( Object argument ) {
	arg = ( String )argument;
    };

    public Object callMethod( int functionId, Object argument ) {
	switch( functionId ) {
	case init_: return init( argument );
	case callalltest_: return callalltest( argument );
	case exchangetest_: return exchangetest( argument );
	case checkInMessage_: return checkInMessage( argument );
	case printOutMessage_: return printOutMessage( argument );
	case printShadow_: return printShadow( argument );
	}
	return null;
    };

    private String arg;
    Object init( Object argument ) { return null; }
    Object callalltest( Object argument ) { return null; }
    Object exchangetest( Object argument ) { return null; }
    Object checkInMessage( Object argument ) { return null; }
    Object printOutMessage( Object argument ) { return null; }
    Object printShadow( Object argument ) { return null; }
}