import java.io.*;
import java.util.*;

import MASS.*;

public class Main {

	public static void main(String[] args) throws Exception{
		if(args.length != 1){
			System.out.println("Usage is JpegAnalyzer [filename.jpg]");
		}

		BlockCollection blockcollection;

		//IMAGE # 1
		//String fileName =  args[0];
		//System.out.println("File to be analyzed_0: " + fileName);
		//JpegAnalyzer analyzer = new JpegAnalyzer(fileName, true, "log.out");

//		String[] fileName = new String[args.length]; //number of images to compare
		ArrayList<JpegAnalyzer> analyzer = new ArrayList<JpegAnalyzer>();
		ArrayList<Pixel[][]> pics = new ArrayList<Pixel[][]>();

		String[] massArgs = new String[4];
			massArgs[0] = "dslab";            // user name
			massArgs[1] = "ds1ab-302";          // password
			massArgs[2] = "machinefile.txt";    // machine file
			massArgs[3] = "";                   // optional proc

			//JpegAnalyzer j = new JpegAnalyzer();
		
		Date startdate = new Date();
              		MASS.init(massArgs, 2, 1);
			Places p = new Places(1, "JpegAnalyzer",(Object) args[0],1, 1);
			// p.callAll(0,args[0]);
			MASS.finish();

			//for(int i = 0; i < args.length; i++){                                                 
			//analyzer.add(new JpegAnalyzer(args[i], true, "log.out")); //create an analyzer         
			//pics.add(analyzer.get(i).pixelData.getpixels()); //get pixels of all pics from args
			//}
		Date enddate = new Date();
	
		System.out.println("time = "+ (enddate.getTime() - startdate.getTime())+ " msec");
		
	/*
		for(int i = 0; i < args.length; i++){
			analyzer.add(new JpegAnalyzer(args[i], true, "log.out")); //create an analyzer
			pics.add(analyzer.get(i).pixelData.getpixels()); //get pixels of all pics from args
		}
	*/
		//blockcollection = new BlockCollection(pics);
		//blockcollection.calcBlockPercent();

//		Pixel[][] pix_0 = analyzer.pixelData.getpixels();
//		Pixel[][] pix_1;

		//print out some pixel values
	//	for(int i = 0; i < 10; i++){
	//		for(int j = 0; j < 10; j++){
				//System.out.println("printing row = " + "column = "+ j);
				
	//			myPixels[i][j].printPixel();
	//		}
	//	} //outer loop
//		
//		System.out.println("MOVING ON TO THE SECOND IMAGE");
			
//		//IMAGE # 2
//		String fileName_1 =  args[1];
  //              System.out.println("File to be analyzed_1: " + fileName);
    //            JpegAnalyzer analyzer_1 = new JpegAnalyzer(fileName_1, true, "log.out");
		
	//	pix_1 = analyzer_1.pixelData.getpixels();

		//****** pixel count and percentage ********

//		ColorImageScale myColors = new ColorImageScale();
//		ColorImageScale col_1 = new ColorImageScale();

//		myColors.setpallet(pix_0, 0,0, 5, 5);
	
//		myColors.printColorPercentage();
//		System.out.println(myColors.getPerRed());
//		System.out.println(myColors.getPerGreen());
//		System.out.println(myColors.getPerBlue());

		

		//col_1.setpallet(pix_1, 10,10);

//		System.out.println("Analyze each block of image");

//		blockcollection.calcBlockPercent(pix_0, pix_1);

		//myColors.printColorPercentage();
		//col_1.printColorPercentage();

		//JpegAnalyzer analyzer = new JpegAnalyzer("C:\\temp\\test.jpg");
		
	}
}
