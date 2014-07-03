public class Mcu {
	
	private int [][] y;
	private int yDc;
	private int [] cr;
	private int crDc;
	private int [] cb;
	private int cbDc;
	private int [] yIndex;
	private int crIndex, cbIndex;
	private int xSub, ySub;

	public Mcu(int newXSub, int newYSub){
		
		// Create a 2d array of 8x8 blocks for y, and one each for cb and cr
		y = new int [newXSub * newYSub][64];
		cr = new int [64];
		cb = new int [64];
		
		yIndex = new int[newXSub * newYSub];
		
		for(int i = 0; i < y.length; i++){
			yIndex[i] = 0;
		}
		
		crIndex = cbIndex = 0;
		for(int i = 0; i < y.length; i++){
			for(int j = 0; j < y[i].length; j++){
				y[i][j] = cb[i] = cr[i] = 0;
			}
		}

		setMode(newXSub, newYSub);
		
	}	
	public void setMode(int x, int y){
		xSub = x;
		ySub = y;
	}
	public int getXMode(){
		return xSub;
	}
	public int getYMode(){
		return ySub;
	}
	public void setY(int targetYBlock, int value){
		if(yIndex[targetYBlock] > 63){
			//System.out.println("Attempted to set a Y value to index > 63					<---");
			return;
		}
		y[targetYBlock][yIndex[targetYBlock]] = value;
		yIndex[targetYBlock]++;
	}
	
	public void setY(int targetYBlock, int value, int index){
		if(index > 63){
			//System.out.println("Attempted to set a Y value to index > 63					<---");
			return;
		}
		y[targetYBlock][index] = value;
	}
	// all values are set to 0 by the constructor, so to fill with
	// 0's we just set the index to the end, indicating that the 
	// Y band is full.
	public void fillY(int targetYBlock){
		yIndex[targetYBlock] = 64;
	}

	public int getYIndex(int targetYBlock){
		return yIndex[targetYBlock];
	}

	public void setCr(int value){
		if(crIndex > 63){
			//System.out.println("Attempted to set a Cr value to index > 63					<---");
			return;
		}
		cr[crIndex++] = value;
	}
	
	public void setCr(int value, int index){
		if(index > 63){
			//System.out.println("Attempted to set a Cr value to index > 63					<---");
			return;
		}
		cr[index] = value;
		//crIndex = index + 1;
	}
	

	public void fillCr(){
		crIndex = 64;
	}


	public void setCb(int value){
		if(cbIndex > 63){
			//System.out.println("Attempted to set a Cb value to index > 63					<---");
			return;
		}
		cb[cbIndex++] = value;
	}
	
	public void setCb(int value, int index){
		if(index > 63){
			//System.out.println("Attempted to set a Cb value to index > 63					<---");
			return;
		}
		cb[index] = value;
		cbIndex = index + 1;
	}
	
	public void fillCb(){
		cbIndex = 64;
	}

	public boolean yFinished(int targetYBlock){
		return (yIndex[targetYBlock] > 63);
	}

	public boolean cbFinished(){
		//System.out.println("index cbFinished cbIndex = " + cbIndex);
		return (cbIndex > 63);
	}

	public boolean crFinished(){
		return  (crIndex > 63);
	}

	public int getY(int targetYBlock, int i){
		return y[targetYBlock][decodeZigZag(i)];
	}
	
	public int getZigZagY(int targetYBlock, int i){
		return y[targetYBlock][i];
	}
	
	public void setZigZagY(int targetYBlock, int index, int value){
		
		y[targetYBlock][decodeZigZag(index)] = value;
		
	}
	public int getCb(int i){
		return cb[decodeZigZag(i)];
	}
	public int getZigZagCb(int i){
		return cb[i];
	}
	public void setZigZagCb(int index, int value){
		
		cb[decodeZigZag(index)] = value;
		
	}
	public int getCr(int i){
		return cr[decodeZigZag(i)];
	}
	public int getZigZagCr(int i){
		return cr[i];
	}
	
	public void setZigZagCr(int index, int value){
		
		cr[decodeZigZag(index)] = value;
		
	}
	
	private int decodeZigZag(int i){
		switch(i){
			case 0 : return 0;
			case 1 : return 1;
			case 2 : return 5;
			case 3 : return 6;
			case 4 : return 14;
			case 5 : return 15;
			case 6 : return 27;
			case 7 : return 28;
			case 8 : return 2;
			case 9 : return 4;
			case 10 : return 7;
			case 11 : return 13;
			case 12 : return 16;
			case 13 : return 26;
			case 14 : return 29;
			case 15 : return 42;
			case 16 : return 3;
			case 17 : return 8; 
			case 18 : return 12; 
			case 19 : return 17; 
			case 20 : return 25; 
			case 21 : return 30;
			case 22 : return 41;
			case 23 : return 43;
			case 24 : return 9; 
			case 25 : return 11; 
			case 26 : return 18; 
			case 27 : return 24; 
			case 28 : return 31; 
			case 29 : return 40; 
			case 30 : return 44; 
			case 31 : return 53; 
			case 32 : return 10; 
			case 33 : return 19; 
			case 34 : return 23; 
			case 35 : return 32; 
			case 36 : return 39; 
			case 37 : return 45; 
			case 38 : return 52; 
			case 39 : return 54; 
			case 40 : return 20; 
			case 41 : return 22; 
			case 42 : return 33; 
			case 43 : return 38; 
			case 44 : return 46; 
			case 45 : return 51; 
			case 46 : return 55; 
			case 47 : return 60; 
			case 48 : return 21; 
			case 49 : return 34; 
			case 50 : return 37; 
			case 51 : return 47; 
			case 52 : return 50; 
			case 53 : return 56; 
			case 54 : return 59; 
			case 55 : return 61; 
			case 56 : return 35; 
			case 57 : return 36; 
			case 58 : return 48; 
			case 59 : return 49; 
			case 60 : return 57; 
			case 61 : return 58; 
			case 62 : return 62; 
			case 63 : return 63;
		}
		return 0;
	} 
}
