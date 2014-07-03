import java.io.IOException;
import java.util.HashMap;
import java.util.*;
import java.util.List;
import java.io.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;

import ucar.ma2.Array;
import ucar.ma2.ArrayBoolean;
import ucar.ma2.ArrayByte;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayLong;
import ucar.ma2.ArrayShort;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import MASS.Place;
import MASS.MASS;
//import MASS.MProcess;

public class Parallel_NetCDF extends Place {
	public static final int init_ = 0;
	public static final int open_ = 1;
	public static final int read_ = 2;
	public static final int write_ = 3;
	public static final int flush_ = 4;
	public static final int close_ = 5;
	public static final int create_uniform = 6;
	public static final int create_custom = 7;

	public static final int type_int = 0;
	public static final int type_long  = 1;
	public static final int type_bool = 2;
	public static final int type_double = 3;
	public static final int type_float = 4;
	public static final int type_byte = 5;
	public static final int type_char = 6;
	public static final int type_short = 7;

	private static int nProc; 			// # processors used
	private static String filename; 	// a NetCDF file name
	private static Object[] buffer; 	// temporarily store netCDF data on memory
	private static DataType[] varInfo;	// store datatype of each variable
	
	private static int fileDataType;	// The data type to use when creating a uniform grid netCDF file
	private static int[] varDims;		// The dimensions of variables for a uniform grid

	private static HashMap<DataType, Integer> mapping;	// Used for retrieving values
	private static boolean preconvert;	// If the file should pre-convert netCDF to java arrays
	private static boolean opened;	// If open() has been called
	private static boolean edited;	// If write() has been successfully called
	private static boolean initialized = false;	// If init() has been called for this Place
	private static List<Variable> allVar;
	private static Configuration conf;
	private static boolean fromHDFS = false;
	private static boolean toHDFS = false;
	
	private Integer increment = new Integer(0); 	//added by sanjay, to increment filename by each writing place obj

	public Parallel_NetCDF() {
		super();
	}

	// Creates an instance of Parallel_NetCDF for the processor it resides on
	// Accepts as a parameter Object arg which is an Object array
	// arg[0] =  the number of processors used by the MASS simulation
	// arg[1] = the name of the file to be read/written
	// arg[2] = the x-axis value of the MASS grid
	// arg[3] = the y-axis value of the MASS grid
	// arg[4] = the data type of the file to be read/written (needed if file must be created)
	public Parallel_NetCDF( Object arg ) {
		super();	// Call superclass constructor
	}

	public Object callMethod( int funcId, Object args ) {
		switch( funcId ) {
		case init_: return init(args);
		case open_: return open(args);
		case read_: return read( args );
		case write_: return write( args );
		case flush_: return flush( );
		case close_: return close( );
		case create_uniform: return create( );
		case create_custom: return create( args );
		}

		return null;
	}

	private Object init(Object arg) {
		// Return if place was already initialized
		if (initialized) return new Integer(0);
		
		Object[] args = ( Object[] ) arg;
		nProc = ( ( Integer ) args[0] ).intValue( );
		filename = (( String ) args[1])+ MASS.getPid();
		fileDataType = ( ( Integer ) args[2] ).intValue( );	
		varDims = ( int[] ) args[3];

		initialized = true;
		opened = false;
		preconvert = false;
		edited = false;
		//conf = new Configuration();
		
		buffer = new Object[ size[0] * size[1] ];		// create buffer array
		varInfo = new DataType[ size[0] * size[1] ];	// create datatype array

		// Set up hashmap for switch statements
		mapping = new HashMap<DataType, Integer>(8);	// Init val = 8 since 8 data types; make static const
		mapping.put(DataType.INT, type_int);
		mapping.put(DataType.LONG, type_long);
		mapping.put(DataType.BOOLEAN, type_bool);
		mapping.put(DataType.DOUBLE, type_double);
		mapping.put(DataType.FLOAT, type_float);
		mapping.put(DataType.BYTE, type_byte);
		mapping.put(DataType.CHAR, type_char);
		mapping.put(DataType.SHORT, type_short);
		
		return new Integer(0);
	}
	
	// arg is a Boolean representing whether the file
	// should convert from netCDF to java arrays upon 
	// opening the file
	private Object open( Object arg ) {
		// Immediately return if file is already open or writer has not been initialized
		

		
		
		if (opened || !initialized) return new Integer(0);	

				
		preconvert = ((Boolean) arg).booleanValue();

		int bufferLoc = index[0] * size[0] + index[1];	// This variable's location in the buffer
		int perProc = (size[0] * size[1]) / nProc;
		int partition = bufferLoc / perProc;
		int firstIndex = partition * perProc;

		// If partitions cannot be even, tack extra indices onto last partition
		if (partition == (nProc - 1)) 
			perProc += (size[0] * size[1]) % nProc;

		NetcdfFile toOpen;
		
		//filename = filename + increment;// + increment.toString();
		if(MASS.getPid() != 0)
			MASS.log("filename: "  + filename);
			
		movefromHDFS();
		synchronized( buffer ) {		
		try {
			//MASS.log("opening file");
			toOpen = NetcdfFile.open(filename);
		} catch (IOException e) {	// File does not exist
			//MASS.log("file does not exist, so attempting to create one");
			boolean wasCreated = ((Boolean)create()).booleanValue();	// Create the file
			
			if (wasCreated) {	// Try to create the file
				try {
					//MASS.log("file created..trying to open");
					toOpen = NetcdfFile.open(filename);
				} catch (IOException f) {	// File has been created but cannot be opened
					//MASS.log("file created, but could not be opened");
					return new Integer(-1);	
				}
			} else { // File was not created 
			//MASS.log("file not created");
				return new Integer(-1);	
			}
		}
	}
		

		allVar = toOpen.getVariables();	// Get variables from the file
		
		synchronized( buffer ) {
			// Loops over the list of variables and stores their Arrays in buffer for quick access
			// Only fills the portion of buffer that belongs to this processor
			for (int i = firstIndex; i < (firstIndex + perProc); i++) {
				if (i < allVar.size()) {	// Only access list if index 'i' is valid
					Variable currVar = allVar.get(i);	// Get the variable at index i
					varInfo[i] = currVar.getDataType();	// Get datatype for variable i

					// If variable can be read, save Array to buffer
					try {
						if (preconvert) {
							buffer[i] = currVar.read().copyToNDJavaArray();
						} else {
							buffer[i] = currVar.read();	
						}

					} catch (IOException e) {
						System.out.println("Variable " + currVar.getFullName() + " could not be read");
						return new Integer(-1);
					}
				}
			}
		}

				
		opened = true;
		return new Integer(0);
	}

	private Object read( Object arg ) { // read data from a NetCDF file
		// Immediately return if file is not open or writer has not been initialized
		if (!opened || !initialized) return new Integer(0);
			

//		Object[] args = (Object[]) arg;
		int[] arrayIndices = (int[]) arg;	
		
		//try{
		//File massLogDir = new File("/net/metis/home3/dslab/MASS/java/appl/NetCDFTest2/MASS_logs/NetCDF.txt" + increment.toString());		
		//FileOutputStream  logger = new FileOutputStream( massLogDir);
		
		//if (!massLogDir.exists()) {
				//massLogDir.createNewFile();
			//}
		
		//String s = arg.toString();
		
		//logger.write( s.concat("\n").getBytes( ) );
		//logger.flush();
		//logger.close();
		//} catch( Exception e ) 
        //{
            //// Unable to Write...
            //System.exit(-1);
        //}
		
		
		
		
		
		int bufferLoc = index[0] * size[0] + index[1];

		DataType currType = varInfo[ bufferLoc ];
		Object currVar = null;

		synchronized( buffer ) {
			if ( buffer == null ) return null; // NetCDF file hasn't been read yet

			// Convert the netCDF array into a correctly dimensioned java array, then return
			if (preconvert)
				currVar = buffer[ bufferLoc ];
			else {
				Array temp = (Array)buffer[ bufferLoc ];
				currVar = temp.copyToNDJavaArray();
			}
		}

	//if(currType == null)	System.exit(-1);
		//if(MASS.myPid == 1) MASS.log(getValue(currType, arrayIndices, currVar).toString());
		return getValue(currType, arrayIndices, currVar);
	}

	// Assumes Object arg is an Object[] where:
	// arg[0] is an int[] of the indices of the value to be written
	// arg[1] is the new value to be stored
	private Object write( Object arg ) { 
		// Immediately return if file is not open or writer has not been initialized
		
		if (!opened || !initialized) return new Integer(0);	
		
		Object[] args = (Object[]) arg;		// Convert parameter to array
		int[] indices = (int[]) args[0];	// get index in the variable to save
		Object toStore = args[1];			// get value to save
		
		//if(MASS.myPid == 1) MASS.log(toStore.toString() + " is being written to file");

		

		int rank = indices.length;
		int bufferLoc = index[0] * size[0] + index[1];

		synchronized( buffer ) {
			DataType currType = varInfo[ bufferLoc ];
			int type = type_double;
			if(mapping.get(currType)!=null)
				type = mapping.get(currType);
				
			boolean success = setNewValue(indices, rank, type, toStore, bufferLoc);
			
			if (success) {
				edited = true;
				return new Integer(0);	// Returns 0 for success
			} else {
				return new Integer(-1);	// Returns 0 for success
			}		
		}
	}

	// write back buffer contents to a NetCDF file.
	// Does not save back to file if no modifications 
	// have been made to buffer's contents
	private Object flush( ) { 
		// Immediately return if file is not open, writer has not been initialized
		// or if nothing has been written to the buffer since it was opened/saved
		if (!opened || !initialized || !edited) return new Integer(0);	
		
		synchronized( buffer ) {
			if ( buffer != null ) { // If a file has been opened
				// write back buffer contents to the corresponding position of a given NetCDF file.

				int bufferLoc = index[0] * size[0] + index[1];
				int perProc = (size[0] * size[1]) / nProc;
				int partition = bufferLoc / perProc;
				int firstIndex = partition * perProc;

				// If partitions cannot be even, tack extra indices onto last partition
				if (partition == (nProc - 1)) 
					perProc += (size[0] * size[1]) % nProc;

				NetcdfFileWriteable toOpen;
				try {
					toOpen = NetcdfFileWriteable.openExisting(filename);
				} catch (IOException e) {	// File does not exist					
					return new Integer(-1);	
				}

				List<Variable> allVar = toOpen.getVariables();	// Get variables from the file
				for (int i = firstIndex; i < (firstIndex + perProc); i++) {
					if (i < allVar.size()) {	// Only access list if index 'i' is valid
						Variable currVar = allVar.get(i);	// Get the variable at index i
						int currType =  mapping.get(varInfo[i]);	// Get datatype for variable i
						String currName = currVar.getFullName();

						int[] shape = currVar.getShape();
						

						try {
							switch(shape.length) {
							case 1:
								switch(currType) {
								case type_int:
									ArrayInt.D1 dataI;
									if (!preconvert) {
										dataI = (ArrayInt.D1) buffer[i];

									} else {
										dataI = (ArrayInt.D1) currVar.read();
										int[] varContentsI = (int[]) buffer[i];

										for (int index = 0; index < varContentsI.length; index++) {
											dataI.set(index, varContentsI[index]);				
										}
									}

									toOpen.write(currName, dataI); // Write to the file
									break;

								case type_long:
									ArrayLong.D1 dataL;
									if (!preconvert) {
										dataL = (ArrayLong.D1) buffer[i];

									} else { 
										dataL = (ArrayLong.D1) currVar.read();
										long[] varContentsL = (long[]) buffer[i];

										for (int index = 0; index < varContentsL.length; index++) {
											dataL.set(index, varContentsL[index]);				
										}

									}

									toOpen.write(currName, dataL); // Write to the file
									break;

								case type_bool:
									ArrayBoolean.D1 dataB;
									if (!preconvert) {
										dataB = (ArrayBoolean.D1) buffer[i];

									} else {
										dataB = (ArrayBoolean.D1) currVar.read();
										boolean[] varContentsB = (boolean[]) buffer[i];

										for (int index = 0; index < varContentsB.length; index++) {
											dataB.set(index, varContentsB[index]);				
										}
									}
									toOpen.write(currName, dataB); // Write to the file
									break;

								case type_double:
									ArrayDouble.D1 dataD;
									if (!preconvert) {
										dataD = (ArrayDouble.D1) buffer[i];

									} else { 
										dataD = (ArrayDouble.D1) currVar.read();
										double[] varContentsD = (double[]) buffer[i];

										for (int index = 0; index < varContentsD.length; index++) {
											dataD.set(index, varContentsD[index]);				
										}
									}
									//if(MASS.myPid == 1) MASS.log(dataD + " is being written to file");

									toOpen.write(currName, dataD); // Write to the file
									break;

								case type_float:
									ArrayFloat.D1 dataF;
									if (!preconvert) {
										dataF = (ArrayFloat.D1) buffer[i];

									} else {
										dataF = (ArrayFloat.D1) currVar.read();
										float[] varContentsF = (float[]) buffer[i];

										for (int index = 0; index < varContentsF.length; index++) {
											dataF.set(index, varContentsF[index]);				
										}
									}

									toOpen.write(currName, dataF); // Write to the file
									break;

								case type_byte:
									ArrayByte.D1 dataByte;
									if (!preconvert) {
										dataByte = (ArrayByte.D1) buffer[i];

									} else {
										dataByte = (ArrayByte.D1) currVar.read();
										byte[] varContentsByte = (byte[]) buffer[i];

										for (int index = 0; index < varContentsByte.length; index++) {
											dataByte.set(index, varContentsByte[index]);				
										}
									}

									toOpen.write(currName, dataByte); // Write to the file
									break;

								case type_char:
									ArrayChar.D1 dataC;
									if (!preconvert) {
										dataC = (ArrayChar.D1) buffer[i];

									} else {
										dataC = (ArrayChar.D1) currVar.read();
										char[] varContentsC = (char[]) buffer[i];

										for (int index = 0; index < varContentsC.length; index++) {
											dataC.set(index, varContentsC[index]);				
										}
									}

									toOpen.write(currName, dataC); // Write to the file
									break;

								case type_short:
									ArrayShort.D1 dataS;
									if (!preconvert) {
										dataS = (ArrayShort.D1) buffer[i];

									} else {
										dataS = (ArrayShort.D1) currVar.read();
										short[] varContentsS = (short[]) buffer[i];

										for (int index = 0; index < varContentsS.length; index++) {
											dataS.set(index, varContentsS[index]);				
										}
									}

									toOpen.write(currName, dataS); // Write to the file
									break;
								}
								break;

							case 2:
								switch(currType) {
								case type_int:
									ArrayInt.D2 dataI;
									if (!preconvert) {
										dataI = (ArrayInt.D2) buffer[i];

									} else {
										dataI = (ArrayInt.D2) currVar.read();
										int[][] varContentsI = (int[][]) buffer[i];

										for (int x = 0; x < varContentsI.length; x++) {
											for (int y = 0; y < varContentsI[x].length; y++) {
												dataI.set(x, y,  varContentsI[x][y]);	
											}
										}
									}

									toOpen.write(currName, dataI); // Write to the file
									break;

								case type_long:
									ArrayLong.D2 dataL;
									if (!preconvert) {
										dataL = (ArrayLong.D2) buffer[i];

									} else {
										dataL = (ArrayLong.D2) currVar.read();
										long[][] varContentsL = (long[][]) buffer[i];

										for (int x = 0; x < varContentsL.length; x++) {
											for (int y = 0; y < varContentsL[x].length; y++) {
												dataL.set(x, y,  varContentsL[x][y]);	
											}
										}
									}

									toOpen.write(currName, dataL); // Write to the file
									break;

								case type_bool:
									ArrayBoolean.D2 dataB;
									if (!preconvert) {
										dataB = (ArrayBoolean.D2) buffer[i];

									} else {
										dataB = (ArrayBoolean.D2) currVar.read();
										boolean[][] varContentsB = (boolean[][]) buffer[i];

										for (int x = 0; x < varContentsB.length; x++) {
											for (int y = 0; y < varContentsB[x].length; y++) {
												dataB.set(x, y,  varContentsB[x][y]);	
											}
										}
									}

									toOpen.write(currName, dataB); // Write to the file
									break;

								case type_double:
									ArrayDouble.D2 dataD;
									if (!preconvert) {
										dataD = (ArrayDouble.D2) buffer[i];

									} else {
										dataD = (ArrayDouble.D2) currVar.read();
										double[][] varContentsD = (double[][]) buffer[i];

										for (int x = 0; x < varContentsD.length; x++) {
											for (int y = 0; y < varContentsD[x].length; y++) {
												dataD.set(x, y,  varContentsD[x][y]);	
											}
										}
									}

									toOpen.write(currName, dataD); // Write to the file
									break;

								case type_float:
									ArrayFloat.D2 dataF;
									if (!preconvert) {
										dataF = (ArrayFloat.D2) buffer[i];

									} else {
										dataF = (ArrayFloat.D2) currVar.read();
										float[][] varContentsF = (float[][]) buffer[i];

										for (int x = 0; x < varContentsF.length; x++) {
											for (int y = 0; y < varContentsF[x].length; y++) {
												dataF.set(x, y,  varContentsF[x][y]);	
											}
										}
									}

									toOpen.write(currName, dataF); // Write to the file
									break;

								case type_byte:
									ArrayByte.D2 dataByte;
									if (!preconvert) {
										dataByte = (ArrayByte.D2) buffer[i];

									} else {
										dataByte = (ArrayByte.D2) currVar.read();
										byte[][] varContentsByte = (byte[][]) buffer[i];

										for (int x = 0; x < varContentsByte.length; x++) {
											for (int y = 0; y < varContentsByte[x].length; y++) {
												dataByte.set(x, y,  varContentsByte[x][y]);	
											}
										}
									}

									toOpen.write(currName, dataByte); // Write to the file
									break;

								case type_char:
									ArrayChar.D2 dataC;
									if (!preconvert) {
										dataC = (ArrayChar.D2) buffer[i];

									} else {
										dataC = (ArrayChar.D2) currVar.read();
										char[][] varContentsC = (char[][]) buffer[i];

										for (int x = 0; x < varContentsC.length; x++) {
											for (int y = 0; y < varContentsC[x].length; y++) {
												dataC.set(x, y,  varContentsC[x][y]);	
											}
										}
									}

									toOpen.write(currName, dataC); // Write to the file
									break;

								case type_short:
									ArrayShort.D2 dataS;
									if (!preconvert) {
										dataS = (ArrayShort.D2) buffer[i];

									} else {
										dataS = (ArrayShort.D2) currVar.read();
										short[][] varContentsS = (short[][]) buffer[i];

										for (int x = 0; x < varContentsS.length; x++) {
											for (int y = 0; y < varContentsS[x].length; y++) {
												dataS.set(x, y,  varContentsS[x][y]);	
											}
										}
									}

									toOpen.write(currName, dataS); // Write to the file
									break;
								}
								break;

							case 3:
								switch(currType) {
								case type_int:
									ArrayInt.D3 dataI;
									if (!preconvert) {
										dataI = (ArrayInt.D3) buffer[i];

									} else {
										dataI = (ArrayInt.D3) currVar.read();
										int[][][] varContentsI = (int[][][]) buffer[i];

										for (int x = 0; x < varContentsI.length; x++) {
											for (int y = 0; y < varContentsI[x].length; y++) {
												for (int z = 0; z < varContentsI[x][y].length; z++) {
													dataI.set(x, y, z,  varContentsI[x][y][z]);	
												}
											}
										}
									}

									toOpen.write(currName, dataI); // Write to the file
									break;

								case type_long:
									ArrayLong.D3 dataL;
									if (!preconvert) {
										dataL = (ArrayLong.D3) buffer[i];

									} else {
										dataL = (ArrayLong.D3) currVar.read();
										long[][][] varContentsL = (long[][][]) buffer[i];

										for (int x = 0; x < varContentsL.length; x++) {
											for (int y = 0; y < varContentsL[x].length; y++) {
												for (int z = 0; z < varContentsL[x][y].length; z++) {
													dataL.set(x, y, z,  varContentsL[x][y][z]);	
												}
											}
										}
									}

									toOpen.write(currName, dataL); // Write to the file
									break;

								case type_bool:
									ArrayBoolean.D3 dataB;
									if (!preconvert) {
										dataB = (ArrayBoolean.D3) buffer[i];

									} else {
										dataB = (ArrayBoolean.D3) currVar.read();
										boolean[][][] varContentsB = (boolean[][][]) buffer[i];

										for (int x = 0; x < varContentsB.length; x++) {
											for (int y = 0; y < varContentsB[x].length; y++) {
												for (int z = 0; z < varContentsB[x][y].length; z++) {
													dataB.set(x, y, z,  varContentsB[x][y][z]);	
												}
											}
										}
									}

									toOpen.write(currName, dataB); // Write to the file
									break;

								case type_double:
									ArrayDouble.D3 dataD;
									if (!preconvert) {
										dataD = (ArrayDouble.D3) buffer[i];

									} else {
										dataD = (ArrayDouble.D3) currVar.read();
										double[][][] varContentsD = (double[][][]) buffer[i];

										for (int x = 0; x < varContentsD.length; x++) {
											for (int y = 0; y < varContentsD[x].length; y++) {
												for (int z = 0; z < varContentsD[x][y].length; z++) {
													dataD.set(x, y, z,  varContentsD[x][y][z]);	
												}	
											}
										}
									}

									toOpen.write(currName, dataD); // Write to the file
									break;

								case type_float:
									ArrayFloat.D3 dataF;
									if (!preconvert) {
										dataF = (ArrayFloat.D3) buffer[i];

									} else {
										dataF = (ArrayFloat.D3) currVar.read();
										float[][][] varContentsF = (float[][][]) buffer[i];

										for (int x = 0; x < varContentsF.length; x++) {
											for (int y = 0; y < varContentsF[x].length; y++) {
												for (int z = 0; z < varContentsF[x][y].length; z++) {
													dataF.set(x, y, z,  varContentsF[x][y][z]);	
												}	
											}
										}
									}

									toOpen.write(currName, dataF); // Write to the file
									break;

								case type_byte:
									ArrayByte.D3 dataByte;
									if (!preconvert) {
										dataByte = (ArrayByte.D3) buffer[i];

									} else {
										dataByte = (ArrayByte.D3) currVar.read();
										byte[][][] varContentsByte = (byte[][][]) buffer[i];

										for (int x = 0; x < varContentsByte.length; x++) {
											for (int y = 0; y < varContentsByte[x].length; y++) {
												for (int z = 0; z < varContentsByte[x][y].length; z++) {
													dataByte.set(x, y, z,  varContentsByte[x][y][z]);	
												}	
											}
										}
									}

									toOpen.write(currName, dataByte); // Write to the file
									break;

								case type_char:
									ArrayChar.D3 dataC;
									if (!preconvert) {
										dataC = (ArrayChar.D3) buffer[i];

									} else {
										dataC = (ArrayChar.D3) currVar.read();
										char[][][] varContentsC = (char[][][]) buffer[i];

										for (int x = 0; x < varContentsC.length; x++) {
											for (int y = 0; y < varContentsC[x].length; y++) {
												for (int z = 0; z < varContentsC[x][y].length; z++) {
													dataC.set(x, y, z,  varContentsC[x][y][z]);	
												}	
											}
										}
									}

									toOpen.write(currName, dataC); // Write to the file
									break;

								case type_short:
									ArrayShort.D3 dataS;
									if (!preconvert) {
										dataS = (ArrayShort.D3) buffer[i];

									} else {
										dataS = (ArrayShort.D3) currVar.read();
										short[][][] varContentsS = (short[][][]) buffer[i];

										for (int x = 0; x < varContentsS.length; x++) {
											for (int y = 0; y < varContentsS[x].length; y++) {
												for (int z = 0; z < varContentsS[x][y].length; z++) {
													dataS.set(x, y, z,  varContentsS[x][y][z]);	
												}	
											}
										}
									}

									toOpen.write(currName, dataS); // Write to the file
									break;
								}
								break;

							case 4:
								switch(currType) {
								case type_int:
									ArrayInt.D4 dataI;
									if (!preconvert) {
										dataI = (ArrayInt.D4) buffer[i];

									} else {
										dataI = (ArrayInt.D4) currVar.read();
										int[][][][] varContentsI = (int[][][][]) buffer[i];

										for (int x = 0; x < varContentsI.length; x++) {
											for (int y = 0; y < varContentsI[x].length; y++) {
												for (int z = 0; z < varContentsI[x][y].length; z++) {
													for (int fourth = 0; fourth < varContentsI[x][y][z].length; fourth++) {
														dataI.set(x, y, z, fourth,  varContentsI[x][y][z][fourth]);
													}
												}
											}
										}
									}
									toOpen.write(currName, dataI); // Write to the file
									break;

								case type_long:
									ArrayLong.D4 dataL;
									if (!preconvert) {
										dataL = (ArrayLong.D4) buffer[i];

									} else {
										dataL = (ArrayLong.D4) currVar.read();
										long[][][][] varContentsL = (long[][][][]) buffer[i];

										for (int x = 0; x < varContentsL.length; x++) {
											for (int y = 0; y < varContentsL[x].length; y++) {
												for (int z = 0; z < varContentsL[x][y].length; z++) {
													for (int fourth = 0; fourth < varContentsL[x][y][z].length; fourth++) {
														dataL.set(x, y, z, fourth,  varContentsL[x][y][z][fourth]);
													}
												}
											}
										}
									}

									toOpen.write(currName, dataL); // Write to the file
									break;

								case type_bool:
									ArrayBoolean.D4 dataB;
									if (!preconvert) {
										dataB = (ArrayBoolean.D4) buffer[i];

									} else {
										dataB = (ArrayBoolean.D4) currVar.read();
										boolean[][][][] varContentsB = (boolean[][][][]) buffer[i];

										for (int x = 0; x < varContentsB.length; x++) {
											for (int y = 0; y < varContentsB[x].length; y++) {
												for (int z = 0; z < varContentsB[x][y].length; z++) {
													for (int fourth = 0; fourth < varContentsB[x][y][z].length; fourth++) {
														dataB.set(x, y, z, fourth,  varContentsB[x][y][z][fourth]);
													}
												}
											}
										}
									}

									toOpen.write(currName, dataB); // Write to the file
									break;

								case type_double:
									ArrayDouble.D4 dataD;
									if (!preconvert) {
										dataD = (ArrayDouble.D4) buffer[i];

									} else {
										dataD = (ArrayDouble.D4) currVar.read();
										double[][][][] varContentsD = (double[][][][]) buffer[i];

										for (int x = 0; x < varContentsD.length; x++) {
											for (int y = 0; y < varContentsD[x].length; y++) {
												for (int z = 0; z < varContentsD[x][y].length; z++) {
													for (int fourth = 0; fourth < varContentsD[x][y][z].length; fourth++) {
														dataD.set(x, y, z, fourth,  varContentsD[x][y][z][fourth]);
													}
												}	
											}
										}
									}

									toOpen.write(currName, dataD); // Write to the file
									break;

								case type_float:
									ArrayFloat.D4 dataF;
									if (!preconvert) {
										dataF = (ArrayFloat.D4) buffer[i];

									} else {
										dataF = (ArrayFloat.D4) currVar.read();
										float[][][][] varContentsF = (float[][][][]) buffer[i];

										for (int x = 0; x < varContentsF.length; x++) {
											for (int y = 0; y < varContentsF[x].length; y++) {
												for (int z = 0; z < varContentsF[x][y].length; z++) {
													for (int fourth = 0; fourth < varContentsF[x][y][z].length; fourth++) {
														dataF.set(x, y, z, fourth,  varContentsF[x][y][z][fourth]);
													}	
												}	
											}
										}
									}

									toOpen.write(currName, dataF); // Write to the file
									break;

								case type_byte:
									ArrayByte.D4 dataByte;
									if (!preconvert) {
										dataByte = (ArrayByte.D4) buffer[i];

									} else {
										dataByte = (ArrayByte.D4) currVar.read();
										byte[][][][] varContentsByte = (byte[][][][]) buffer[i];

										for (int x = 0; x < varContentsByte.length; x++) {
											for (int y = 0; y < varContentsByte[x].length; y++) {
												for (int z = 0; z < varContentsByte[x][y].length; z++) {
													for (int fourth = 0; fourth < varContentsByte[x][y][z].length; fourth++) {
														dataByte.set(x, y, z, fourth,  varContentsByte[x][y][z][fourth]);
													}	
												}	
											}
										}
									}

									toOpen.write(currName, dataByte); // Write to the file
									break;

								case type_char:
									ArrayChar.D4 dataC;
									if (!preconvert) {
										dataC = (ArrayChar.D4) buffer[i];

									} else {
										dataC = (ArrayChar.D4) currVar.read();
										char[][][][] varContentsC = (char[][][][]) buffer[i];

										for (int x = 0; x < varContentsC.length; x++) {
											for (int y = 0; y < varContentsC[x].length; y++) {
												for (int z = 0; z < varContentsC[x][y].length; z++) {
													for (int fourth = 0; fourth < varContentsC[x][y][z].length; fourth++) {
														dataC.set(x, y, z, fourth,  varContentsC[x][y][z][fourth]);
													}	
												}	
											}
										}
									}

									toOpen.write(currName, dataC); // Write to the file
									break;

								case type_short:
									ArrayShort.D4 dataS;
									if (!preconvert) {
										dataS = (ArrayShort.D4) buffer[i];

									} else {
										dataS = (ArrayShort.D4) currVar.read();
										short[][][][] varContentsS = (short[][][][]) buffer[i];

										for (int x = 0; x < varContentsS.length; x++) {
											for (int y = 0; y < varContentsS[x].length; y++) {
												for (int z = 0; z < varContentsS[x][y].length; z++) {
													for (int fourth = 0; fourth < varContentsS[x][y][z].length; fourth++) {
														dataS.set(x, y, z, fourth,  varContentsS[x][y][z][fourth]);
													}
												}	
											}
										}
									}

									toOpen.write(currName, dataS); // Write to the file
									break;
								}
								break;
							}								
						} catch (IOException e) {
							return new Integer(-1);
						} catch (InvalidRangeException e) {
							return new Integer(-1);
						}
					}
				}
					
				// Close the file
				try {
					toOpen.close();
				} catch (IOException e) {
					return new Integer(-1);
				}
			}
		}

		edited = false;
		return new Integer(0);	// Returns 0 for success
	}
	
private boolean movefromHDFS(){

	if(buffer!=null)
	synchronized( buffer ) {
      
      
      if ( fromHDFS == false) {
         fromHDFS = true; //so that only one thread ever enters this loop
         
         MASS.log("started movefromHDFS");
         conf = new Configuration();
         conf.set("fs.default.name","hdfs://hercules.uwb.edu:20511");
         
         
         conf.addResource(new Path("/net/metis/home3/dslab/hadoop/conf/core-site.xml"));
         
         FileSystem fs = null;
         try{
            fs = FileSystem.get(conf);
         }catch (IOException e) {
            MASS.log("EXECPTION");
         }
         
         Path in = new Path(filename);
         Path out = new Path("hdfs://hercules.uwb.edu:20511/" + filename);
         
         
         
         try{
            if(!fs.exists(out)) return false;
            fs.copyToLocalFile(true,out,in);
            fs.close();
         }catch (Exception e) {
			 
            //MASS.log("EXECPTION2" );
         }
         MASS.log("read file from hadoop completed!");
         
      }
   }
   
   
   
   return true;
   
}
	
	
	
	private void movetoHDFS(){
  
  if(buffer!=null)
   synchronized( buffer ) {
      
      
      if ( toHDFS == false) {
         toHDFS = true;
         
         MASS.log("started movetoHDFS");
         conf = new Configuration();
         conf.set("fs.default.name","hdfs://hercules.uwb.edu:20511");
         
         
         conf.addResource(new Path("/net/metis/home3/dslab/hadoop/conf/core-site.xml"));
         
         FileSystem fs = null;
         try{
            fs = FileSystem.get(conf);
         }catch (IOException e) {
            MASS.log("EXECPTION");
         }
         
         Path in = new Path(filename);
         Path out = new Path("hdfs://hercules.uwb.edu:20511/" + filename);
                  
         try{
            
            fs.copyFromLocalFile(true,true,in,out);
            fs.close();
         }catch (IOException e) {
            MASS.log("EXECPTION2" );
         }
               MASS.log("Writing file to Hadoop completed!");
               

      }
   }
   
   
   
   
   
}
	

	private Object close(){
		flush();	
		movetoHDFS();
		buffer = null;
		varInfo = null;
		opened = false;
		initialized = false;

		return new Integer(0);
	}

	private synchronized boolean setNewValue(int[] indices, int rank, int type, Object newVal, int bufLoc) {
		switch (rank) {
		case 1: 
			switch (type) {
			case type_int:
				int toWriteI = ((Integer) newVal).intValue();
	
				if (preconvert) {
					int[] curr = (int[]) buffer[bufLoc];
					curr[indices[0]] = toWriteI;
					buffer[bufLoc] = (Object) curr;
				} else {
					ArrayInt.D1 dataI = (ArrayInt.D1) buffer[bufLoc];
					dataI.set(indices[0], toWriteI);
					buffer[bufLoc] = (Object) dataI;
				}
				return true;
				
			case type_long:
				long toWriteL = ((Long) newVal).longValue();
				
				if (preconvert) {
					long[] curr = (long[]) buffer[bufLoc];
					curr[indices[0]] = toWriteL;
					buffer[bufLoc] = (Object) curr;
				} else {
					ArrayLong.D1 dataL = (ArrayLong.D1) buffer[bufLoc];
					dataL.set(indices[0], toWriteL);
					buffer[bufLoc] = (Object) dataL;
				}
				return true;

			case type_bool:
				boolean toWriteB = ((Boolean) newVal).booleanValue();
				
				if (preconvert) {
					boolean[] curr = (boolean[]) buffer[bufLoc];
					curr[indices[0]] = toWriteB;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayBoolean.D1 dataB = (ArrayBoolean.D1) buffer[bufLoc];
					dataB.set(indices[0], toWriteB);
					buffer[bufLoc] = (Object) dataB;
				}
				return true;

			case type_double:
				double toWriteD = ((Double) newVal).doubleValue();
				
				if (preconvert) {
					double[] curr = (double[]) buffer[bufLoc];
					curr[indices[0]] = toWriteD;
					buffer[bufLoc] = (Object) curr;
					
				} else {					
					ArrayDouble.D1 dataD = (ArrayDouble.D1) buffer[bufLoc];
					dataD.set(indices[0], toWriteD);
					buffer[bufLoc] = (Object)dataD;
				}
				return true;

			case type_float:
				float toWriteF = ((Float) newVal).floatValue();

				if (preconvert) {
					float[] curr = (float[]) buffer[bufLoc];
					curr[indices[0]] = toWriteF;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayFloat.D1 dataF = (ArrayFloat.D1) buffer[bufLoc];
					dataF.set(indices[0], toWriteF);
					buffer[bufLoc] = (Object)dataF;
				}
				return true;

			case type_byte:
				byte toWriteByte = ((Byte) newVal).byteValue();
				
				if (preconvert) {
					byte[] curr = (byte[]) buffer[bufLoc];
					curr[indices[0]] = toWriteByte;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayByte.D1 dataByte = (ArrayByte.D1) buffer[bufLoc];
					dataByte.set(indices[0], toWriteByte);
					buffer[bufLoc] = (Object)dataByte;
				}
				return true;

			case type_char:
				char toWriteC = ((Character) newVal).charValue();
				
				if (preconvert) {
					char[] curr = (char[]) buffer[bufLoc];
					curr[indices[0]] = toWriteC;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayChar.D1 dataC = (ArrayChar.D1) buffer[bufLoc];
					dataC.set(indices[0], toWriteC);
					buffer[bufLoc] = (Object)dataC;
				}
				return true;

			case type_short:
				short toWriteS = ((Short) newVal).shortValue();
				
				if (preconvert) {
					short[] curr = (short[]) buffer[bufLoc];
					curr[indices[0]] = toWriteS;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayShort.D1 dataS = (ArrayShort.D1) buffer[bufLoc];
					dataS.set(indices[0], toWriteS);
					buffer[bufLoc] = (Object)dataS;
				}
				return true;
			}

		case 2: 
			switch (type) {
			case type_int:
				int toWriteI = ((Integer) newVal).intValue();
				
				if (preconvert) {
					int[][] curr = (int[][]) buffer[bufLoc];
					curr[indices[0]][indices[1]] = toWriteI;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayInt.D2 dataI = (ArrayInt.D2) buffer[bufLoc];
					dataI.set(indices[0], indices[1], toWriteI);
					buffer[bufLoc] = (Object)dataI;
				}
				return true;

			case type_long:
				long toWriteL = ((Long) newVal).longValue();
				
				if (preconvert) {
					long[][] curr = (long[][]) buffer[bufLoc];
					curr[indices[0]][indices[1]] = toWriteL;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayLong.D2 dataL = (ArrayLong.D2) buffer[bufLoc];
					dataL.set(indices[0], indices[1], toWriteL);
					buffer[bufLoc] = (Object)dataL;
				}
				return true;

			case type_bool:
				boolean toWriteB = ((Boolean) newVal).booleanValue();
				
				if (preconvert) {
					boolean[][] curr = (boolean[][]) buffer[bufLoc];
					curr[indices[0]][indices[1]] = toWriteB;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayBoolean.D2 dataB = (ArrayBoolean.D2) buffer[bufLoc];
					dataB.set(indices[0], indices[1], toWriteB);
					buffer[bufLoc] = (Object)dataB;
				}
				return true;

			case type_double:
				double toWriteD = ((Double) newVal).doubleValue();
				
				if (preconvert) {
					double[][] curr = (double[][]) buffer[bufLoc];
					curr[indices[0]][indices[1]] = toWriteD;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayDouble.D2 dataD = (ArrayDouble.D2) buffer[bufLoc];
					dataD.set(indices[0], indices[1], toWriteD);
					buffer[bufLoc] = (Object)dataD;
				}
				return true;

			case type_float:
				float toWriteF = ((Float) newVal).floatValue();

				if (preconvert) {
					float[][] curr = (float[][]) buffer[bufLoc];
					curr[indices[0]][indices[1]] = toWriteF;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayFloat.D2 dataF = (ArrayFloat.D2) buffer[bufLoc];
					dataF.set(indices[0], indices[1], toWriteF);
					buffer[bufLoc] = (Object)dataF;
				}
				return true;

			case type_byte:
				byte toWriteByte = ((Byte) newVal).byteValue();
				
				if (preconvert) {
					byte[][] curr = (byte[][]) buffer[bufLoc];
					curr[indices[0]][indices[1]] = toWriteByte;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayByte.D2 dataByte = (ArrayByte.D2) buffer[bufLoc];
					dataByte.set(indices[0], indices[1], toWriteByte);
					buffer[bufLoc] = (Object)dataByte;
				}
				return true;

			case type_char:
				char toWriteC = ((Character) newVal).charValue();
				
				if (preconvert) {
					char[][] curr = (char[][]) buffer[bufLoc];
					curr[indices[0]][indices[1]] = toWriteC;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayChar.D2 dataC = (ArrayChar.D2) buffer[bufLoc];
					dataC.set(indices[0], indices[1], toWriteC);
					buffer[bufLoc] = (Object)dataC;
				}
				return true;

			case type_short:
				short toWriteS = ((Short) newVal).shortValue();
				
				if (preconvert) {
					short[][] curr = (short[][]) buffer[bufLoc];
					curr[indices[0]][indices[1]] = toWriteS;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayShort.D2 dataS = (ArrayShort.D2) buffer[bufLoc];
					dataS.set(indices[0], indices[1], toWriteS);
					buffer[bufLoc] = (Object)dataS;
				}
				return true;
			}
			
		case 3: 
			switch (type) {
			case type_int:
				int toWriteI = ((Integer) newVal).intValue();
				
				if (preconvert) {
					int[][][] curr = (int[][][]) buffer[bufLoc];
					curr[indices[0]][indices[1]][indices[2]] = toWriteI;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayInt.D3 dataI = (ArrayInt.D3) buffer[bufLoc];
					dataI.set(indices[0], indices[1], indices[2], toWriteI);
					buffer[bufLoc] = (Object)dataI;
				}
				return true;

			case type_long:
				long toWriteL = ((Long) newVal).longValue();
				
				if (preconvert) {
					long[][][] curr = (long[][][]) buffer[bufLoc];
					curr[indices[0]][indices[1]][indices[2]] = toWriteL;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayLong.D3 dataL = (ArrayLong.D3) buffer[bufLoc];
					dataL.set(indices[0], indices[1], indices[2], toWriteL);
					buffer[bufLoc] = (Object)dataL;
				}
				return true;

			case type_bool:
				boolean toWriteB = ((Boolean) newVal).booleanValue();
				
				if (preconvert) {
					boolean[][][] curr = (boolean[][][]) buffer[bufLoc];
					curr[indices[0]][indices[1]][indices[2]] = toWriteB;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayBoolean.D3 dataB = (ArrayBoolean.D3) buffer[bufLoc];
					dataB.set(indices[0], indices[1], indices[2], toWriteB);
					buffer[bufLoc] = (Object)dataB;
				}
				return true;

			case type_double:
				double toWriteD = ((Double) newVal).doubleValue();
				
				if (preconvert) {
					double[][][] curr = (double[][][]) buffer[bufLoc];
					curr[indices[0]][indices[1]][indices[2]] = toWriteD;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayDouble.D3 dataD = (ArrayDouble.D3) buffer[bufLoc];
					dataD.set(indices[0], indices[1], indices[2], toWriteD);
					buffer[bufLoc] = (Object)dataD;
				}
				return true;

			case type_float:
				float toWriteF = ((Float) newVal).floatValue();

				if (preconvert) {
					float[][][] curr = (float[][][]) buffer[bufLoc];
					curr[indices[0]][indices[1]][indices[2]] = toWriteF;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayFloat.D3 dataF = (ArrayFloat.D3) buffer[bufLoc];
					dataF.set(indices[0], indices[1], indices[2], toWriteF);
					buffer[bufLoc] = (Object)dataF;
				}
				return true;

			case type_byte:
				byte toWriteByte = ((Byte) newVal).byteValue();
				
				if (preconvert) {
					byte[][][] curr = (byte[][][]) buffer[bufLoc];
					curr[indices[0]][indices[1]][indices[2]] = toWriteByte;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayByte.D3 dataByte = (ArrayByte.D3) buffer[bufLoc];
					dataByte.set(indices[0], indices[1], indices[2], toWriteByte);
					buffer[bufLoc] = (Object)dataByte;
				}
				return true;

			case type_char:
				char toWriteC = ((Character) newVal).charValue();
				
				if (preconvert) {
					char[][][] curr = (char[][][]) buffer[bufLoc];
					curr[indices[0]][indices[1]][indices[2]] = toWriteC;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayChar.D3 dataC = (ArrayChar.D3) buffer[bufLoc];
					dataC.set(indices[0], indices[1], indices[2], toWriteC);
					buffer[bufLoc] = (Object)dataC;
				}
				return true;

			case type_short:
				short toWriteS = ((Short) newVal).shortValue();
				
				if (preconvert) {
					short[][][] curr = (short[][][]) buffer[bufLoc];
					curr[indices[0]][indices[1]][indices[2]] = toWriteS;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayShort.D3 dataS = (ArrayShort.D3) buffer[bufLoc];
					dataS.set(indices[0], indices[1], indices[2], toWriteS);
					buffer[bufLoc] = (Object)dataS;
				}
				return true;
			}
			
		case 4: 
			switch (type) {
			case type_int:
				int toWriteI = ((Integer) newVal).intValue();
				
				if (preconvert) {
					int[][][][] curr = (int[][][][]) buffer[bufLoc];
					curr[indices[0]][indices[1]][indices[2]][indices[3]] = toWriteI;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayInt.D4 dataI = (ArrayInt.D4) buffer[bufLoc];
					dataI.set(indices[0], indices[1], indices[2], indices[3], toWriteI);
					buffer[bufLoc] = (Object)dataI;
				}
				return true;

			case type_long:
				long toWriteL = ((Long) newVal).longValue();
				
				if (preconvert) {
					long[][][][] curr = (long[][][][]) buffer[bufLoc];
					curr[indices[0]][indices[1]][indices[2]][indices[3]] = toWriteL;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayLong.D4 dataL = (ArrayLong.D4) buffer[bufLoc];
					dataL.set(indices[0], indices[1], indices[2], indices[3], toWriteL);
					buffer[bufLoc] = (Object)dataL;
				}
				return true;

			case type_bool:
				boolean toWriteB = ((Boolean) newVal).booleanValue();
				
				if (preconvert) {
					boolean[][][][] curr = (boolean[][][][]) buffer[bufLoc];
					curr[indices[0]][indices[1]][indices[2]][indices[3]] = toWriteB;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayBoolean.D4 dataB = (ArrayBoolean.D4) buffer[bufLoc];
					dataB.set(indices[0], indices[1], indices[2], indices[3], toWriteB);
					buffer[bufLoc] = (Object)dataB;
				}
				return true;

			case type_double:
				double toWriteD = ((Double) newVal).doubleValue();
				
				if (preconvert) {
					double[][][][] curr = (double[][][][]) buffer[bufLoc];
					curr[indices[0]][indices[1]][indices[2]][indices[3]] = toWriteD;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayDouble.D4 dataD = (ArrayDouble.D4) buffer[bufLoc];
					dataD.set(indices[0], indices[1], indices[2], indices[3], toWriteD);
					buffer[bufLoc] = (Object)dataD;
				}
				return true;

			case type_float:
				float toWriteF = ((Float) newVal).floatValue();

				if (preconvert) {
					float[][][][] curr = (float[][][][]) buffer[bufLoc];
					curr[indices[0]][indices[1]][indices[2]][indices[3]] = toWriteF;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayFloat.D4 dataF = (ArrayFloat.D4) buffer[bufLoc];
					dataF.set(indices[0], indices[1], indices[2], indices[3], toWriteF);
					buffer[bufLoc] = (Object)dataF;
				}
				return true;

			case type_byte:
				byte toWriteByte = ((Byte) newVal).byteValue();
				
				if (preconvert) {
					byte[][][][] curr = (byte[][][][]) buffer[bufLoc];
					curr[indices[0]][indices[1]][indices[2]][indices[3]] = toWriteByte;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayByte.D4 dataByte = (ArrayByte.D4) buffer[bufLoc];
					dataByte.set(indices[0], indices[1], indices[2], indices[3], toWriteByte);
					buffer[bufLoc] = (Object)dataByte;
				}
				return true;

			case type_char:
				char toWriteC = ((Character) newVal).charValue();
				
				if (preconvert) {
					char[][][][] curr = (char[][][][]) buffer[bufLoc];
					curr[indices[0]][indices[1]][indices[2]][indices[3]] = toWriteC;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayChar.D4 dataC = (ArrayChar.D4) buffer[bufLoc];
					dataC.set(indices[0], indices[1], indices[2], indices[3], toWriteC);
					buffer[bufLoc] = (Object)dataC;
				}
				return true;

			case type_short:
				short toWriteS = ((Short) newVal).shortValue();
				
				if (preconvert) {
					short[][][][] curr = (short[][][][]) buffer[bufLoc];
					curr[indices[0]][indices[1]][indices[2]][indices[3]] = toWriteS;
					buffer[bufLoc] = (Object) curr;
					
				} else {
					ArrayShort.D4 dataS = (ArrayShort.D4) buffer[bufLoc];
					dataS.set(indices[0], indices[1], indices[2], indices[3], toWriteS);
					buffer[bufLoc] = (Object)dataS;
				}
				return true;
			}
		}
		
		return false;
	}

	// Assumes that 'values' is an ND java array 
	private Object getValue(DataType type, int[] location, Object values) {
		// Does not check 'preconvert' since read uses tonDArray in read before calling
		// this, so it is always called with a java array
		int typeNum = mapping.get(type);
		return getJavaValue(typeNum, location, values);
	}

	// Assumes that 'values' is an ND java array 
	// Assumes that 'location' is a valid index into the desired array
	private Object getJavaValue(int typeSwitch, int[] location, Object values) {
		
		
		
        
        int rank = location.length;

		switch (rank) {
		case 1:
			switch (typeSwitch) {
			case 0:
				int[] dataI = (int[]) values;
				return dataI[ location[0] ];

			case 1:
				long[] dataL = (long[]) values;
				return dataL[ location[0] ];

			case 2:
				boolean[] dataB = (boolean[]) values;
				return dataB[ location[0] ];

			case 3:
				double[] dataD = (double[]) values;
				return dataD[ location[0] ];

			case 4:
				float[] dataF = (float[]) values;
				return dataF[ location[0] ];

			case 5:
				byte[] dataByte = (byte[]) values;
				return dataByte[ location[0] ];

			case 6:
				char[] dataC = (char[]) values;
				return dataC[ location[0] ];

			case 7:
				short[] dataS = (short[]) values;
				return dataS[ location[0] ];
			}

		case 2:
			switch (typeSwitch) {
			case 0:
				int[][] dataI = (int[][]) values;
				return dataI[ location[0] ][ location[1] ];

			case 1:
				long[][] dataL = (long[][]) values;
				return dataL[ location[0] ][ location[1] ];

			case 2:
				boolean[][] dataB = (boolean[][]) values;
				return dataB[ location[0] ][ location[1] ];

			case 3:
				double[][] dataD = (double[][]) values;
				return dataD[ location[0] ][ location[1] ];

			case 4:
				float[][] dataF = (float[][]) values;
				return dataF[ location[0] ][ location[1] ];

			case 5:
				byte[][] dataByte = (byte[][]) values;
				return dataByte[ location[0] ][ location[1] ];

			case 6:
				char[][] dataC = (char[][]) values;
				return dataC[ location[0] ][ location[1] ];

			case 7:
				short[][] dataS = (short[][]) values;
				return dataS[ location[0] ][ location[1] ];
			}

		case 3:
			switch (typeSwitch) {
			case 0:
				int[][][] dataI = (int[][][]) values;
				return dataI[ location[0] ][ location[1] ][ location[2] ];

			case 1:
				long[][][] dataL = (long[][][]) values;
				return dataL[ location[0] ][ location[1] ][ location[2] ];

			case 2:
				boolean[][][] dataB = (boolean[][][]) values;
				return dataB[ location[0] ][ location[1] ][ location[2] ];

			case 3:
				double[][][] dataD = (double[][][]) values;
				return dataD[ location[0] ][ location[1] ][ location[2] ];

			case 4:
				float[][][] dataF = (float[][][]) values;
				return dataF[ location[0] ][ location[1] ][ location[2] ];

			case 5:
				byte[][][] dataByte = (byte[][][]) values;
				return dataByte[ location[0] ][ location[1] ][ location[2] ];

			case 6:
				char[][][] dataC = (char[][][]) values;
				return dataC[ location[0] ][ location[1] ][ location[2] ];

			case 7:
				short[][][] dataS = (short[][][]) values;
				return dataS[ location[0] ][ location[1] ][ location[2] ];
			}

		case 4:
			switch (typeSwitch) {
			case 0:
				int[][][][] dataI = (int[][][][]) values;
				return dataI[ location[0] ][ location[1] ][ location[2] ][ location[3] ];

			case 1:
				long[][][][] dataL = (long[][][][]) values;
				return dataL[ location[0] ][ location[1] ][ location[2] ][ location[3] ];

			case 2:
				boolean[][][][] dataB = (boolean[][][][]) values;
				return dataB[ location[0] ][ location[1] ][ location[2] ][ location[3] ];

			case 3:
				double[][][][] dataD = (double[][][][]) values;
				return dataD[ location[0] ][ location[1] ][ location[2] ][ location[3] ];

			case 4:
				float[][][][] dataF = (float[][][][]) values;
				return dataF[ location[0] ][ location[1] ][ location[2] ][ location[3] ];

			case 5:
				byte[][][][] dataByte = (byte[][][][]) values;
				return dataByte[ location[0] ][ location[1] ][ location[2] ][ location[3] ];

			case 6:
				char[][][][] dataC = (char[][][][]) values;
				return dataC[ location[0] ][ location[1] ][ location[2] ][ location[3] ];

			case 7:
				short[][][][] dataS = (short[][][][]) values;
				return dataS[ location[0] ][ location[1] ][ location[2] ][ location[3] ];
			}
		}

		// If it gets to here without returning, then the data cannot be retrieved
		System.out.println("Variable is an unsupported data type");
		return null;
	}


	// Creates a 2-Dimensional or 3-Dimensional NetCDF file
	// Returns true if the file is created and no problems are encountered. 
	// Returns false otherwise
	// Parameters:
	// * int[] dimensions : are the dimensions each variable generated will have
	// * int[] gridDimensions: File dimensions legnth, width, and depth (if 3D), in that order
	// * String fileName : name that the new file will have
	// * DataType datatype : the kind of data that the variables will store
	// * boolean addUnlimited : whether each variable will also have an unlimited dimension
	private Object create() {
		// Immediately return if writer has not been initialized
		if (!initialized) return new Integer(0);	
		
		NetcdfFileWriteable currentFile;
		try {
			currentFile = NetcdfFileWriteable.createNew(filename);

			// Add file dimensions to aid reading later
			currentFile.addDimension("FileLength", size[1]);
			currentFile.addDimension("FileWidth", size[0]);
			if (size.length > 2) 
				currentFile.addDimension("FileDepth", size[2]);

			// Create and fill dimension array
			Dimension[] newDims = new Dimension[varDims.length];
			for (int i = 0; i < varDims.length; i++) {
				newDims[i] = currentFile.addDimension("dim" + i, varDims[i]);
			}
			
			DataType type = getType(fileDataType);
			String currName = "";
			
			// create variables in a grid according to parameters
			// gridDimensions[0] = number of rows
			// gridDimensions[1] = number of columns
			// gridDimensions[2] = depth of grid
			for (int i = 0; i < size[0]; i++) {
				for (int j = 0; j < size[1]; j++) {
					if (size.length > 2) {	// If there is a third dimension
						for (int k = 0; k < size[2]; k++) {
							currName = i + "-" + j + "-" + k;
						}
					} else {	// If there are 2 dimensions						
						currName = i + "-" + j;
					}
					currentFile.addVariable(currName, type, newDims);	// Write to file
				}
			}
			currentFile.create();	// Generate the file
			
		} catch (IOException e2) {
			System.out.println("Error creating file for data");
			return new Boolean(false);
		} 
		
		return new Boolean(true);
	}
	
	private Object create( Object arg ) {
		Object[] args = (Object[]) arg;
		filename = (String) args[0];
		int numVar = ((Integer) args[1]).intValue();
		int[] dataTypes = (int[]) args[2];
		int[][] variableDims = (int[][]) args[3];
		String[] varNames = (String[]) args[4];
		
		NetcdfFileWriteable newFile = null;
		try {
			newFile = NetcdfFileWriteable.createNew(filename);

			// Create each variable in the file according to user input
			for (int i = 0; i < numVar; i++) {
				// Create array of dimensions for this variable
				Dimension[] currDims = new Dimension[variableDims[i].length];
				for (int j = 0; j < variableDims[i].length; j++) {
					currDims[j] = newFile.addDimension("dim" + i + "-" + j, variableDims[i][j]);
				}
				
				// Get the dataType of the variable
				DataType currType = getType(dataTypes[i]);
				newFile.addVariable(varNames[i], currType, currDims);	// Write to file
			}

			newFile.create();	// Generate the file

		} catch (IOException e2) {
			System.out.println("Error creating file for data");
			return new Boolean(false);
		} finally {		// Close out the file no matter what happens
			if (newFile != null)
				try {
					newFile.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
					return new Boolean(false);
				}
		}
		return new Boolean(true);
	}
	
	private DataType getType(int type) {
		switch(type) {
		case type_int:
			return DataType.INT;
		case type_long:
			return DataType.LONG;
		case type_bool:
			return DataType.BOOLEAN;
		case type_double:
			return DataType.DOUBLE;
		case type_float:
			return DataType.FLOAT;
		case type_byte:
			return DataType.BYTE;
		case type_char:
			return DataType.CHAR;
		case type_short:
			return DataType.SHORT;
		default: 
			System.out.println("Incorrect data type");
			return null;
		}
	}
}

