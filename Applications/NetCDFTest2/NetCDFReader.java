// To use: java -cp netcdfAll-4.2.jar:. NetCDFReader string int
// string = *.nc within the same folder
// int = number of variables displayed per line

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
//import java.io.PrintWriter;

import java.util.List;

//import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
//import ucar.ma2.InvalidRangeException;
//import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;


public class NetCDFReader {	
	public static void main(String args[]) {
		String filename = args[0];
		
		// Open the file and read each variable contained within
		NetcdfFile fileIn = null;
		try {
			fileIn = NetcdfFile.open(filename, null);	// Open the file
			List<Variable> varList = fileIn.getVariables();	// Get list of file's variables
			
			System.out.println("Number of variables in file: " + varList.size());
			System.out.println();
			
			// ~~~~~~~~~~~~~ Read each variables ~~~~~~~~~~~~~ //
			double sum = 0.0;
			for (int i = 0; i < varList.size(); i++) {
				Variable curr = varList.get(i);		// Get the current Variable
				System.out.print(curr.getFullName());
				
				ArrayDouble.D1 values = (ArrayDouble.D1)curr.read();
				for (int j = 0; j < values.getSize(); j++) {
					System.out.println(" " + values.get(j));

					sum += values.get(j);
				}
			}

			System.out.println();
			System.out.println("Sum of contents: " + sum);
			// ~~~~~~~~~~~~~ Done ~~~~~~~~~~~~~ //		

		} catch (java.io.IOException e) {
			e.printStackTrace();
			return;
		} finally {		// Close out the file no matter what occurs
			if (fileIn != null)
				try {
					fileIn.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
		}
	}
	
//	// Get a file name from the console
//	// Returns a String from System.in
//	public static String getFileName() {
//		System.out.print("File to open: ");		// Print prompt
//		BufferedReader input = new BufferedReader(new InputStreamReader(System.in)); 
//		String toReturn = null;
//		
//		try {
//			toReturn = input.readLine();	// Get file name
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
//		
//		System.out.println();
//		return toReturn;
//	}
}
