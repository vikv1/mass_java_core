import java.io.*;
import java.util.*;

// MASS.*;

public class Main {

	public static void main(String[] args) throws Exception{
		if(args.length < 2){
			System.out.println("Invalid number of arguments: enter at least 2 image (file) names");
			System.out.println("Usage is JpegAnalyzer [filename.jpg]");
		}

		BlockCollection blockcollection;

		String[] fileName = new String[args.length]; //number of images to compare
		ArrayList<JpegAnalyzer> analyzer = new ArrayList<JpegAnalyzer>();
		ArrayList<Pixel[][]> pics = new ArrayList<Pixel[][]>();

		Date startdate = new Date();
			for(int i = 0; i < args.length; i++){
				analyzer.add(new JpegAnalyzer(args[i], true, "log.out")); //create an analyzer
				pics.add(analyzer.get(i).pixelData.getpixels()); //get pixels of all pics from args
				fileName[i] = args[i];
			}
		Date enddate = new Date();
		System.out.println("JpegAnalyzer time = "+ (enddate.getTime() - startdate.getTime())+ " msec");

		Date start = new Date();
			blockcollection = new BlockCollection(pics, fileName);
			//sorting here
			blockcollection.calcBlockPercent();
		Date end = new Date();
		System.out.println("Comparison time = "+ (end.getTime() - start.getTime())+ " msec");
	
	} //end of main
} //end of class

