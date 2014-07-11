package MASS;

import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.reflect.*;

public class Places_base {
    //Used to toggle comments from Places_base.java
    //private static final boolean printOutput = false;
    private static final boolean printOutput = true;

    public Places_base( int handle, String className, int boundary_width,
			Object argument, int[] size ) {
	this.handle = handle;
	this.className = className;
	this.boundary_width = boundary_width;
	this.size = size;

	if ( printOutput == true )
	    MASS_base.log( "Places_base handle = " + handle
			   + ", class = " + className
			   + ", argument = " + argument
			   + ", boundary_width = " + boundary_width 
			   + ", size.length = " + size.length );

	init_all( argument );
    }

    public void init_all( Object argument ) {
	// For debugging
	if ( printOutput == true ) {
	    MASS_base.log( "init_all handle = " + handle + 
			   ", class = " + className + 
			   ", argument = " + argument );

	    String convert = null;
	    for ( int i = 0; i < size.length; i++ )
		convert += ( "size[" + i + "] = " + size[i] + "  " );
	    MASS_base.log(  convert );

	    // Print the current working directory
	    MASS_base.log( "CUR_DIR = " + MASS_base.CUR_DIR );

	    // load the place construtor
	    File curDir   = new File( MASS.CUR_DIR );
	    try {
		placeLoader =
		    URLClassLoader.
		    newInstance( new URL[] { curDir.toURI().toURL( ) } );
		placeClass =                                        //get class
		    Class.forName( className, true, placeLoader ); 
		placeConstructor =                            //get constructor
		    placeClass.getConstructor( Object.class ); 
		
		// calculate lower_boundary and upper_boundary
		int total = 1;
		for ( int i = 0; i < size.length; i++ )
		    total *= size[i];
		int stripe = total / MASS_base.systemSize;
		
		lower_boundary = stripe * MASS_base.myPid;
		upper_boundary = (MASS_base.myPid < MASS_base.systemSize - 1) ?
		    lower_boundary + stripe - 1 : total - 1;
		places_size = upper_boundary - lower_boundary + 1;
		
		// instantiate Places objects
		this.places_size = places_size;
		//  maintaining an entire set
		places = new Place[places_size];
		
		// initialize all Places objects
		for ( int i = 0; i < places_size; i++ ) {
		    // instanitate a new place
		    placeInitSize = size.clone( );
		    placeInitIndex = getGlobalArrayIndex( lower_boundary + i );
		    places[i] = 
			( Place )placeConstructor.newInstance( argument );
		}
	    } catch ( Exception e ) {
		MASS_base.log( "Places_base.init_all: " + className + 
			       " not loaded and/or instantiated " + e );
	    }

	    // allocate the left/right shadows
	    MASS_base.log( "Places_base.init_all: left/right shadows skip" );
	}
    }

    protected int[] getGlobalArrayIndex( int singleIndex ) {
	int[] index = new int[size.length];

	for ( int i = size.length - 1; i >= 0; i-- ) {
	    // calculate from lower dimensions
	    index[i] = singleIndex % size[i];
	    singleIndex /= size[i];
	}

	return index;
    }

    public void callAll( int functionId, Object argument, int tid ) {
    }

    public Object callAll( int functionId, Object arguments, int length,
			   int tid ) {
	return null;
    }

    public void exchangeAll( Places_base dstPlaces, int functionId,
			     Vector<int[]> destinations, int tid ) {
    }

    public void exchangeBoundary( ) {
    }

    private void getLocalRange( int[] range, int tid ) {
    }

    protected final int handle;
    protected final String className;
    
    protected int lower_boundary;
    protected int upper_boundary;
    protected int places_size;
    protected int[] size;
    protected int shadow_size;
    protected int boundary_width;
    
    private static URLClassLoader placeLoader;
    private Class<?> placeClass;
    private Constructor<?> placeConstructor;
    private Place[] places;
    
    public static int[] placeInitIndex;
    public static int[] placeInitSize;
}