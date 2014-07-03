public class Simple {
    public static void main( String[] args ) {
	for (Thread t : Thread.getAllStackTraces().keySet()) {
	    if (t.getState()==Thread.State.RUNNABLE) {
		System.out.println( t.toString( ) );
	    }
	}
    }
}