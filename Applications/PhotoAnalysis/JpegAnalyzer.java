import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.util.Vector;

//import MASS.*;


public class JpegAnalyzer{  //extends Place {
	
	private int currentIndex;
	private int lineCount;
	private int samplesPerLine;

	private byte currentByte;
	private byte readBuff[];
	private boolean logging;
	private PrintWriter logFile;
	private Vector<Scan> scans;
	private Vector<Frame> frames;
	public PixelData pixelData;  //modified to public -- raj
	private Pixel [][] pixels;   //modified to public --- raj

	// The huffman tables are first indexed by id, then by class. Id's can range from 0 to 3,
	// and classes are either 0 or 1.
	private HuffmanTable huffmanTables [] [];
	private QuantizationTable quantizationTables [];

	private static final int byte1 = 0xFF;
	private static final int byte2 = 0xFF00;

	JpegAnalyzer(){}
	
	JpegAnalyzer(String fileName, boolean logSwitch, String logFileName){		
		
		logging = logSwitch;
		if(logging){
			try{
				logFile = new PrintWriter(new FileOutputStream(logFileName), true);
			} catch(Exception e){
				//System.out.println(e);
			}
		}

		huffmanTables = new HuffmanTable[4][3];
		quantizationTables = new QuantizationTable[3];
		frames = new Vector<Frame>(1);

		currentIndex = 0;
		FileInputStream iStream;
		try{
			iStream = new FileInputStream(fileName);
			readBuff = new byte [1048576];
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			int readCount = iStream.read(readBuff);
			while(readCount > 0){
				bout.write(readBuff, 0, readCount);
				readCount = iStream.read(readBuff);
			}
			
			// Read the first byte, and post-increment the index
		} catch(Exception e){
			//System.out.println(e);
		}
		while(tagExists()){
			currentByte = readBuff[currentIndex++];
			if(currentByte == (byte)(0xFF)){
				// Control code
				readControlCode();
			} else {
				//System.out.println("Non control code : " + currentByte);

			}
		}

		convertMCUs();

	}
/*
	public JpegAnalyzer init(String fileName){
		return (new JpegAnalyzer(fileName, true, "log.out"));
	}

	public JpegAnalyzer callMethod(int funcId, String fileName){
		switch(funcId){
			case 0: return init(fileName);
		}
		return null;
	}
*/
	private void convertMCUs(){

		pixelData.quantizeY(quantizationTables[0]);
		pixelData.quantizeCbCr(quantizationTables[1]);
		pixelData.populatePixels();
		
		System.out.println("Conversion complete");
	}
	private void readControlCode(){
		currentByte = readBuff[currentIndex++];
		//System.out.println("Reading control code");
		if(currentByte >= (byte)(0xE0) && currentByte <= (byte)(0xEF)){
			//System.out.println("parsing app data");
			parseApplicationData();
		} else {
			switch(currentByte){
				//FFC0 - FFC3 Nondifferential huffman-coding frames
				case((byte)(0XC0))	:	//System.out.println("SOF 0 Baseline DCT");
										parseBaselineDCT();
										break;
				case((byte)(0XC1))	:	//System.out.println("SOF 1 Extended Sequential DCT");
										break;
				case((byte)(0XC2))	:	//System.out.println("SOF 2 Progressive DCT");
										break;
				case((byte)(0XC3))	:	//System.out.println("SOF 3 Lossless(sequential)");
										break;
				//FFC4 DHT Define Huffman Table
				case((byte)(0XC4))	:	//System.out.println("DHT Define Huffman Table");
										parseDHT();
										break;
				//FFC5 - FFC7 Differential Huffman Coding Frames
				case((byte)(0XC5))	:	//System.out.println("SOF 5 Differential Sequential DCT");
										break;
				case((byte)(0XC6))	:	//System.out.println("SOF 6 Differential Progressive DCT");
										break;
				case((byte)(0XC7))	:	//System.out.println("SOF 7 Differential lossless");
										break;
				//FFC8 Reserved for jpeg extensions
				case((byte)(0xC8))	:	//System.out.println("Reserved for JPG extensions");
										break;
				//FFC9 - FFcB Nondifferential arithmetic-coding frames
				case((byte)(0xC9))	:	//System.out.println("Extended sequential DCT");
										break;
				case((byte)(0xCA))	:	//System.out.println("Progressive DCT");
										break;
				case((byte)(0xCB))	:	//System.out.println("Lossless (Sequential)");
										break;
				//FFCC Define arthmetic conditioning table
				case((byte)(0xCC))	:	//System.out.println("DAC");
										break;
				//FFCD - FFCF Differential Arithmetic Coding frames
				case((byte)(0xCD))	:	//System.out.println("Differential Sequential DCT");
										break;
				case((byte)(0xCE))	:	//System.out.println("Differential Progressive DCT");
										break;
				case((byte)(0xCF))	:	//System.out.println("Differential Lossless");
										break;
				case((byte)(0xD8))	: 	//System.out.println("Start of image");
										break;

				case((byte)(0xDA))	: 	//System.out.println("Start of Scan");
										parseSOS();
										break;
				// FFDB Define Quantization Table
				case((byte)(0xDB))	:	//System.out.println("DQT");
										parseQuantizationTable();
										break;
				// EOI End of image
				case((byte)(0xD9))	:	//System.out.println("EOI End of Image");
										if(logging){
											logFile.println("EOI at " + currentIndex);
										}
										break;
				// FFFE Comment
				case((byte)(0xFE))	: 	parseComment();
										break;
				default			  	: 	//System.out.println("Unrecognized");
			}
		}
	}	
	private void parseApplicationData(){
		// we don't need application data, so we just read the length, and move past it.
		// the length field in the marker includes the two length bytes, so we subtract
		// one to find the correct index, since the currentIndex should indicate the last byte
		// that was read.
		currentIndex += getLength();
	}
	
	private void parseComment(){
		currentIndex += getLength();
	}
	
	private int getLength(){
		
		byte lowOrder, highOrder;
		highOrder = readBuff[currentIndex++];
		lowOrder = readBuff[currentIndex++];
		// Create length by converting both bytes to uints, then add them together. High order byte's
		// value is multiplied by 2^8 to effectively bitshift it over 
		int length = (byteToUint(highOrder) * 256) + byteToUint(lowOrder);
		//System.out.println("Length of segment " + length);
		return length;
	}
	private int byteToUint(byte in){
		int intVal = (int)(in);
		if(intVal < 0){
			intVal += 256;
		}
		return intVal;
	}
	private boolean tagExists(){
		// Check to see if there's room for a tag
		//System.out.println("Checking for tag at byte: " + (currentIndex));
		if((currentIndex + 2) > readBuff.length){
			//System.out.println("No more room for another tag");
			return false;
		} else {
			// look for a new marker code
			byte marker;
			marker = readBuff[currentIndex];
			if (marker != ((byte)(0xFF))){
				//System.out.println("No more tags");
				return false;
			}
		}
		return true;
	}
	private void parseQuantizationTable(){
			// I think we can just skip length all together
			currentIndex += 2;
		do { 
			QuantizationTable newQT = new QuantizationTable();
			//newQT.setLength(getLength());
			//System.out.println("reading byte: " + currentIndex);
			byte precisionAndId = readBuff[currentIndex++];
			newQT.setPrecision((short)(precisionAndId >> 4));
			newQT.setID((int)(precisionAndId & 0xF));
			//System.out.println("ID = " + newQT.getID());
			if(newQT.getPrecision() == (short)(0)){
				logFile.println("8 bit values");
				for(int i = 0; i < 64; i++){
					newQT.addElement((short)(readBuff[currentIndex++]));
				}
			} else {
				for(int i = 0; i < 64; i++){
					logFile.println("16 bit values");
					short temp = (short)(readBuff[currentIndex++]);
					temp = (short)(temp << 8);
					temp = (short)(temp | readBuff[currentIndex++]);
					newQT.addElement(temp);
				}	
			}

			quantizationTables[newQT.getID()] = newQT;
			
			if(readBuff[currentIndex] == (byte)(0xFF)){
				break;
			}

		} while (true);
	}
	private void parseBaselineDCT(){
		if(logging){
			logFile.println("SOF 0 BaseLine DCT " + currentIndex + "-" + (currentIndex + getLength()));
			currentIndex -= 2;
		}

		//Skip the length field
		currentIndex += 2;
		int precision = byteToUint(readBuff[currentIndex++]);
		lineCount = byteToUint(readBuff[currentIndex++]);
		lineCount *= 256;
		lineCount += byteToUint(readBuff[currentIndex++]);
		samplesPerLine = byteToUint(readBuff[currentIndex++]);
		samplesPerLine *= 256;
		samplesPerLine += byteToUint(readBuff[currentIndex++]);
		//System.out.println("Samples per line: " + samplesPerLine + "  Linecount: " + lineCount);
		pixelData = new PixelData(samplesPerLine, lineCount);
		pixels = new Pixel[samplesPerLine][lineCount];
		//System.out.println("Initialized pixel array to " + samplesPerLine + ", " + lineCount);
		int componentsPerFrame = byteToUint(readBuff[currentIndex++]);

		Frame newFrame = new Frame(precision, lineCount, samplesPerLine, componentsPerFrame);

		for(int i = 0; i < componentsPerFrame; i++){
			int componentId = byteToUint(readBuff[currentIndex++]);
			int horizFactor = (int)(readBuff[currentIndex] >> 4);
			int vertFactor = (int)(readBuff[currentIndex++] & (byte)(0x0F));
			int quantSelector = byteToUint(readBuff[currentIndex++]);
			newFrame.addComponent(componentId, horizFactor, vertFactor, quantSelector);
		}

		
		frames.add(newFrame);
	}
	private void parseDHT(){
		if(logging){
			logFile.println("DHT Define Huffman Table at " + currentIndex + "-" + (currentIndex + getLength()));
			currentIndex -= 2;
		}

		// We're calling getLength() to simply advance the currentIndex past the length field
		getLength();

		do{
		
			HuffmanTable newHT = new HuffmanTable();
	
			// read and set table class
			byte tableClassAndID = readBuff[currentIndex++];
			newHT.setClass(byteToUint((byte)((tableClassAndID & 0xF0) >> 4)));
	
			// read and set table id
			newHT.setID(byteToUint((byte)(tableClassAndID & 0xF)));
	
			// read and set length counts
			for(int i = 1; i <= 16; i++){
				newHT.setCodeLengthCount(i, byteToUint(readBuff[currentIndex++]));
			}
	
			// the codeList will be where we store codes for later organization into the huffman binary
			// tree
			Vector<Short> codeList = new Vector<Short>();
		
			int codeCount = 0;
			for(int i = 1; i <= 16; i++){
				codeCount += newHT.getCodeLengthCount(i);
			}
	
			// Read the bytes that have the 8-bit codes in them
			for(int i = 0; i < codeCount; i++){
				codeList.add((short)(readBuff[currentIndex++]));
			}
		
			// call populate tree here, then maybe have a call in HT that prints out the values to test
			newHT.populateCodeTable(codeList);
	

			//System.out.println("Setting table at ID: " + newHT.getID() + " and class : " + newHT.getTableClass());
			huffmanTables[newHT.getID()][newHT.getTableClass()] = newHT;
			if(readBuff[currentIndex] == (byte)(0xFF)){
				break;
			}
		} while(true);
	}
	private void parseSOS(){
		if(logging){
			logFile.println("SOS Start of Scan at " + currentIndex + "-" + (currentIndex + getLength()));
			currentIndex -= 2;
		}

		//skipping length field
		currentIndex += 2;
		int componentCount = byteToUint(readBuff[currentIndex++]);
		Scan newScan = new Scan(componentCount);

		// TODO:
		// this should call a method in the frame to modify the components which live there,
		// rather than storing component data in two different data structures.
		for(int i = 0; i < componentCount; i++){
			int selector = byteToUint(readBuff[currentIndex++]);
			int dcSelector = (int)(readBuff[currentIndex] >> 4);
			int acSelector = (int)(readBuff[currentIndex++] & (byte)(0x0F));
			newScan.addComponent(selector, dcSelector, acSelector);
		}

		newScan.setStartOfSelection(byteToUint(readBuff[currentIndex++]));
		newScan.setEndOfSelection(byteToUint(readBuff[currentIndex++]));
		newScan.setBitHigh((int)(readBuff[currentIndex] >> 4));
		newScan.setBitLow((int)(readBuff[currentIndex++] & (byte)(0x0F)));

		// Get the y sampling values from the frame	
		// TODO: We need to find a way to handle multiple frames
		int hFactor = frames.get(0).getComponentHFactor(0);
		int vFactor = frames.get(0).getComponentVFactor(0);

		if(hFactor == 2 && vFactor == 2){
			// 2x2
			readScan(4,2,0);
		} else if (hFactor == 1 && vFactor == 1){
			// 1x1
			readScan(4,4,4);
		} else {
			//System.out.println("Chroma subsampling ratio not recognized/implemented");
			System.exit(0);
		}

		// Looking for the next control tag
		byte high, low;
		do{
			high = readBuff[currentIndex];
			low = readBuff[currentIndex + 1];
			if(high == (byte)(0xFF) && low != (byte)(0x00)){
				if(logging){
					logFile.println("End of Scan at " + currentIndex);
				}
				return;
			}
			currentIndex++;
		} while(true);
	}
	private String byteToString(byte input){
		String output = "";
		int intVal = byteToUint(input);
		int msb = intVal >>> 4;
		msb &= 0xF;
		if(msb < 9){
			output +=  msb;
		} else {
			output += (char)('A' + (msb % 10));
		}
		int lsb = intVal & 0xF;
		if(lsb < 9){
			output += lsb;
		} else {
			output += (char)('A' + (lsb% 10));
		}
		return output;
	}
	short bytesToUshort(byte low, byte high){
		short result = (short)(Math.pow((double)(byteToUint(high)), 4.0));
		result += (short)(byteToUint(low));
		return result;
	}
	int huffDCDifferences(byte low, byte high, int bits){
		int difference = bytesToUshort(low, high);
		
		if(difference < Math.pow(2, bits)){
			difference = 1 - (int)(Math.pow(2.0, (double)(bits)));
		}

		return difference;
	}

	// Reads a scan
	// Params: the three ratio numbers for chroma subsampling.
	// The ratios are usually:
	// 4:4:4 No subsamping
	// 4:2:2 2x1 Horizontal subsampling
	// 4:2:0 2x2 Vertical and horizontal
	void readScan(int r1, int r2, int r3){
		int ssss, tableClass, id, samplingType, yBlocks, xSampleWidth, ySampleHeight, value,
		    currentY, buffer, bufferShiftCounter, temp, current, dc, chromaIndex;
		temp = current = buffer = bufferShiftCounter = ssss = tableClass = id = yBlocks = 
	        xSampleWidth = ySampleHeight = currentY = value = dc = chromaIndex = 
	        samplingType = 0;
	
		Mcu newMcu = null;


		/// These booleans keep track of where we are in the components of the scan.
		// the SSSS value is the first code in a scan, and it tells us how many bits the
		// dc value is, so we need to handle the first two codes differently.
		boolean endOfScan, yFinished, cbFinished, crFinished, readingDC, ssssUnknown;
		endOfScan =  yFinished = cbFinished = readingDC = false;
		
		// crFinished is initially true so we can set up the MCUs
		ssssUnknown = crFinished = true;

		// Initialize the first 2 bytes into current
		temp = temp | readScanByte();
		temp = temp & 255;
		current = current | temp;
		current = current << 8;
		temp = 0;
		temp = temp | readScanByte();
		temp = temp & 255;
		current = current | temp;
	
		// Initialize the buffer
		buffer = current;
		for(int i = 0; i < 2; i++){
			temp = 0;
			temp = temp | readScanByte(); 
			temp = temp & 255;
			buffer = buffer << 8;
			buffer = buffer | temp;
		}

		do{
			// if cr block is finished then we need to reset for reading another MCU
				if(crFinished){
					crFinished = cbFinished = yFinished = readingDC = false;
					ssssUnknown = true;
					id = tableClass = currentY = 0;
					
					// This check is to make sure we're not just executing the three previous statements to reset
					// the state of the method to read a new MCU. Probably can be eliminated.
					if(newMcu != null){
						////System.out.println("Writing array to pixelData...");
						////System.out.println("Current index = " + currentIndex);
						writeMCUToData(newMcu, xSampleWidth, ySampleHeight);
						//System.out.println("Success");
						newMcu = null;
					}
					
					// No chroma subsampling. Indicated by samplingType = 0
					if(r1 == 4 && r2 == 4 && r3 == 4){
						newMcu = new Mcu(1, 1);
						samplingType = 0;
						yBlocks = 1;
						xSampleWidth = 1;
						ySampleHeight = 1;
					// 2x2 chroma subsampling. Indicated by samplingType = 1
					} else if(r1 == 4 && r2 == 2 && r3 == 0){
						newMcu = new Mcu(2, 2);
						samplingType = 1;
						yBlocks = 4;
						xSampleWidth = 2;
						ySampleHeight = 2;
					}
				}

			// readingAC provides a flag for when we're reading DC a value. These
			// values aren't huffman coded, and ssss provides their length, so
			// we need to use this boolean when reading the next scan data.
			
			int shiftCounter = 16;
			value = 0;

			// If we know ssss but not dc, then we've only read the first code,
			// which corresponds to ssss. Now we need to read the next ssss bits,
			// and convert those into a dc code. We set the shiftCounter accordingly.
			if(readingDC){
				
				// The one is added because we don't need to decrement the shift counter, since
				// the length is already known
				shiftCounter = 16 - ssss + 1;
			}
		
			do{
				shiftCounter--;
				//current = current | (buffer >>> 16);
				//current = current >>> shiftCounter;

				temp = 0;
				temp = temp | (buffer >>> 16);
				temp = temp >>> shiftCounter;
				/*	InputStreamReader input = new InputStreamReader(System.in);
					BufferedReader is = new BufferedReader(input);
					try{
						is.readLine();
					} catch (Exception e){}
				*/
					// If we read a ac code, we just need to break, and not check for a huffman value

				if(readingDC){
					dc = temp;
					break;
				}
				if(huffmanTables[id][tableClass].getValue(temp, 16 - shiftCounter) != null){
					value = (Short)(huffmanTables[id][tableClass].getValue(temp, 16 - shiftCounter));
					if(value < 0){
						value = value & 255;
					}
					////System.out.println("Decoded a " + value);
					
//					if(value == 240){
//						//System.out.println("Encountered a ZRL");
//					}
					break;
				}
				
				if(shiftCounter == 0){
					//System.out.println("Decoding failed");
					break;
				}
			} while(true);

			current = current << (16 - shiftCounter);
			current = current & (byte1 | byte2);

			// Initialize temp
			temp = temp | (buffer >>> 16);

			// Shift temp over so it aligns with the empty space in current
			temp = temp >>> (16 - shiftCounter);
			current = current | temp;

			// Shift buffer over to load correct bits into temp
			temp = 0;
			temp = temp | (buffer >>> (16 - shiftCounter));
		
			// Shift the buffer over one bit at a time. Every 8th shift requires us to load
			// a new byte from the data.
			for(int i = 0; i < 16 - shiftCounter; i++){
				buffer = buffer << 1;
				bufferShiftCounter++;
				if(bufferShiftCounter == 8){
					temp = 0;
					temp = buffer | readScanByte(); 
					temp = temp & 255;
					buffer = buffer | temp;
					bufferShiftCounter = 0;
				}
			}

			// If we know the ssss, but not the dc, then we need to read the next ssss bits of
			// the scan and store them as the dc.
			if(readingDC){
				dc = getDCCoefficient(dc, ssss);
				//System.out.println("dc = " + dc);
				readingDC = false;
				
				// We'll be reading AC values next, and their table class is 1
				tableClass = 1;
				if(!yFinished){
					newMcu.setY(currentY, dc);
					if(newMcu.yFinished(currentY)){
						
						//System.out.println("Finished y " + currentY);
						// Go to the next mcu, and set state variables to read another y block
						currentY++;
						
						ssssUnknown = true;
						id = tableClass = 0;
						
						// If currentY is equal to the number of Y mcus, then we've
						// finished reading y, and need to start over for cb and cr. currentMCU
						// is set to 0. Table class is set to 0, and table id is set to 1, corresponding
						// to the huffman code table for cr and cb DC values.
						if(currentY == yBlocks){
							//System.out.println("y finished. Switching to Cb.");
							yFinished = true;
							id = 1; 
							tableClass = 0;
							currentY = 0;
						}
					}
					
				} else if(!cbFinished){
					//writeCbValue(dc, chromaIndex, newMcu, samplingType);
					newMcu.setCb(dc);
					chromaIndex++;
					if(newMcu.cbFinished()){
						//System.out.println("Cb block finished, switching to Cr");
						ssssUnknown = true;
						cbFinished = true;
						id = 1;
						tableClass = 0;
						chromaIndex = 0;
					}
					
				} else {
					//writeCrValue(dc, chromaIndex, mcuArray, samplingType);
					newMcu.setCr(dc);
					chromaIndex++;
					if(newMcu.crFinished()){
						crFinished = true;
						//System.out.println("Cr finished");
						chromaIndex = 0;
					}
				}

			// If we don't know the ssss, then we need to save it, then read the DC
			} else if(ssssUnknown){
				//System.out.println("ssss = " + value);
				ssss = value;
				ssssUnknown = false;
				readingDC = true;
			} else { 

			// Write the Y values to all the MCUs
				if(!yFinished){
					if(value == 0){
						newMcu.fillY(currentY);
					} else {
						int rrrr;
						if(value == 240){
							rrrr = 16;
							readingDC = false;
						} else {
							rrrr = runSizeToRrrr(value);
							ssss = runSizeToSsss(value);
							readingDC = true;
						}
						
						while(rrrr > 0){
							newMcu.setY(currentY, 0);
							rrrr--;
						}
						// TODO readingDC should really be readingDCMP
						
					}
		
					if(newMcu.yFinished(currentY)){
						//System.out.println("Finished y " + currentY);
						// Go to the next mcu, and set state variables to read another y block
						currentY++;
						ssssUnknown = true;
						id = tableClass = 0;
						
						// If the current MCU index is equal to the number of mcus, then we've
						// finished reading y, and need to start over for cb and cr. currentMCU
						// is set to 0. Table class is set to 0, and table id is set to 1, corresponding
						// to the huffman code table for cr and cb DC values.
						if(currentY == yBlocks){
							//System.out.println("y finished. Switching to Cb.");
							yFinished = true;
							id = 1; 
							tableClass = 0;
							currentY = 0;
						}
					}
					
			// Y is finished, so now we are working on the cb values
				} else if (!cbFinished){
					
					// 0 indicated EOB, so set the rest of the cb vals to 0
					if(value == 0){
						newMcu.fillCb();
					} else {
						int rrrr;
						if(value == 240){
							rrrr = 16;
							readingDC = false;
						} else {
							rrrr = runSizeToRrrr(value);
							readingDC = true;
							ssss = runSizeToSsss(value);
						}
						
						
						while(rrrr > 0){
							//writeCbValue(0, chromaIndex, mcuArray, samplingType);
							newMcu.setCb(0);
							rrrr--;
							chromaIndex++;
						}
						// TODO readingDC should really be readingDCMP
					}

					// Check if cb has been filled for the last mcu, if so, then cb is done
					if(newMcu.cbFinished()){
						//System.out.println("Cb is finished with EOB, going to Cr");
						ssssUnknown = true;
						cbFinished = true;
						id = 1;
						tableClass = 0;
						chromaIndex = 0;
					}
			// cb is finished, so we now are working on the cr values
				} else {
					if(value == 0){
						newMcu.fillCr();

					} else {
						int rrrr;
						if(value == 240){
							rrrr = 16;
							readingDC = false;
						
						} else {
							rrrr = runSizeToRrrr(value);
							readingDC = true;
							ssss = runSizeToSsss(value);
						}
						while(rrrr > 0){
							//writeCrValue(0, chromaIndex, mcuArray, samplingType);
							newMcu.setCr(0);
							rrrr--;
							chromaIndex++;
						}
							// TODO readingDC should really be readingDCMP
							
						
					}
					// Check if the cr in the last mcu is full. If so, cr is done
					if(newMcu.crFinished()){
						crFinished = true;
						//System.out.println("Cr finished");
						chromaIndex = 0;
					}
					
					// If the next byte is the end of scan marker, then the rest of the buffer is stuffed 1's,
					// which we can discard.
					if(readBuff[currentIndex] == (byte)(0xFF) && readBuff[currentIndex + 1] == (byte)(0xD9)){
						writeMCUToData(newMcu, xSampleWidth, ySampleHeight);
						break;
					}
				}
			}
		// Check for end of scan here
	}while(true);
		// Check for remainder bits. If we find them, then we should break
	}
	// Reading bytes out of scan data is tricky because of stuffed bits
	private byte readScanByte(){
		byte next = readBuff[currentIndex++];
	//	//System.out.println("reading byte : " + next + " at index: " + (currentIndex - 1));
		if (next == (byte)(0xFF)){
			if(readBuff[currentIndex] != (byte)(0)){
				//System.out.println("Encountered a tag in scan data.");
				//System.out.println("Index = " + currentIndex);
				//System.out.println("Value = " + readBuff[currentIndex]);
				currentIndex--;
				return (byte)(0x0);
			} else {
				//System.out.println("discarding a stuff byte");
				currentIndex++;
			}
		}
		return next;
	}
	private int runSizeToRrrr(int runSize){
		int rrrr = runSize / 16;
		return rrrr;
	}
	private int runSizeToSsss(int runSize){
		int ssss = runSize % 16;
		return ssss; 
	}
	private int getDCCoefficient(int value, int length){
		if (value >= Math.pow(2, length - 1)){
			return value;
		} else {
			return (value - (int)((Math.pow(2,length) - 1)));
		}
	}

	private void writeMCUToData(Mcu newMcu, int xWidth, int yWidth){
		pixelData.setMode(xWidth, yWidth);

		//TODO: Finish how pixelData works
		pixelData.addMcu(newMcu);				

	}
	private void writeCrValue(int value, int index, Mcu[] mcuArray, int samplingType){
		
		int targetMcu = 0;
		
		switch(samplingType){
			// Case 0, no subsampling
			case 0: break;
			// Case 1, 2x2 subsampling
			case 1:
				
				// Targeting mcu 0 or 1
				if(index / 8 < 4){
					if(index % 8 < 4){
						targetMcu = 0;
					} else {
						targetMcu = 1;
						index -= 4;
					}
				} else {
					if(index % 8 < 4){
						targetMcu = 2;
						index -= 32;
					} else {
						targetMcu = 3;
						index -= 36;
					}
				}
				mcuArray[targetMcu].setCr(value, index * 2);
				mcuArray[targetMcu].setCr(value, index * 2 + 1);
				mcuArray[targetMcu].setCr(value, index * 2 + 8);
				mcuArray[targetMcu].setCr(value, index * 2 + 9);
				break;
			default:
		}
		
		
		
	}
	private void writeCbValue(int value, int index, Mcu[] mcuArray, int samplingType){
	
		int targetMcu = 0;
		
		switch(samplingType){
			// Case 0, no subsampling
			case 0: break;
			// Case 1, 2x2 subsampling
			case 1:
				// Targeting mcu 0 or 1
				if(index / 8 < 4){
					if(index % 8 < 4){
						targetMcu = 0;
					} else {
						targetMcu = 1;
						index -= 4;
					}
				} else {
					if(index % 8 < 4){
						targetMcu = 2;
						index -= 32;
					} else {
						targetMcu = 3;
						index -= 36;
					}
				}
				mcuArray[targetMcu].setCb(value, index * 2);
				mcuArray[targetMcu].setCb(value, index * 2 + 1);
				mcuArray[targetMcu].setCb(value, index * 2 + 8);
				mcuArray[targetMcu].setCb(value, index * 2 + 9);
				break;
			default:
		}
	}
	/*
	public static void main(String[] args) {
		if(args.length != 1){
			System.out.println("Usage is JpegAnalyzer [filename.jpg]");
		}

		String fileName =  args[0];
		System.out.println("File to be analyzed: " + fileName);
		Date start = new Date();
		JpegAnalyzer analyzer = new JpegAnalyzer(fileName, true, "log.out");
		Date end = new Date();
		Long timeElapsed = end.getTime() - start.getTime(); 
		System.out.println("Time Elapsed: " + timeElapsed);
	}
*/
}	
