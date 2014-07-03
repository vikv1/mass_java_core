

import java.io.*;
import java.util.ArrayList;
import java.text.*;
import java.lang.Math;

public class BlockCollection{

	ArrayList<ImageBlockData> closenessFactor = new ArrayList<ImageBlockData>(); //closeness factor
	ArrayList<Pixel[][]> images = new ArrayList<Pixel[][]>(); //image after analyzer call (pixel values)
	ArrayList<ImageBlockData> block_data; //each image's block data after comparison of each block (relative values)
	ComparisonResults[] results; //consists of image-name and its closness factor 
	String[] files;

	int delta_x = 0;
	int delta_y = 0;
	final int REGION_SIZE = 10; //each image's will be divided into 10x10 (total 100 blocks)

	public BlockCollection(ArrayList<Pixel[][]> pics, String[] fileName){

		block_data = new ArrayList<ImageBlockData>(pics.size());
		files = new String[fileName.length];
		results = new ComparisonResults[fileName.length - 1];

		for(int i = 0; i< pics.size(); i++){
			images.add(pics.get(i));
			block_data.add(new ImageBlockData());
			closenessFactor.add(new ImageBlockData());
			files[i] = fileName[i];
		} //copy all the images into my local array
	}

	//calculate the percentage of red green and blue for each block
	public void calcBlockPercent(){
		
		for(int i = 0; i < images.size(); i++){ //for each image
			Pixel[][] pic = images.get(i);
			delta_x = pic.length / REGION_SIZE;			
			delta_y = pic[0].length / REGION_SIZE;
			ColorImageScale temp; // = new ColorImageScale();

			//int dup = checkDuplicateImage(i);
		//	if( dup >= 0 ){
			//	block_data.get(i).setBlockArray(block_data.get(dup).getBlockArray());
			//	continue;
			//}
			
			for(int x = 0; x < pic.length; x+= delta_x){
				for(int y = 0; y <pic[0].length; y+=delta_y){
					temp = new ColorImageScale();					
					temp.setpallet(images.get(i), x, y, delta_x, delta_y);
					block_data.get(i).addBlock(new myPix(temp.getPerRed(), temp.getPerGreen(), temp.getPerBlue()));
					temp.reset();
				}
			}

		} //image

		calculateClosenessFactor();
		summation();
		results = merge_sort(results);

		for(int i = 0; i < results.length; i++){
			results[i].printResult();
		}

	} //end of calcBlockPercentage
	
	private int checkDuplicateImage(int imageIndex){
		int dupIndex = -1;
		for(int i = 0; i <= imageIndex; i++){
			if(imageIndex == i){
				continue;
			}
			if(files[imageIndex].equals(files[i])){
				dupIndex = i;
			}
		}
		return dupIndex;
	} //end of duplicate check

	private void calculateClosenessFactor(){
		//closeness factor
		double max, min, r, g, b;		

		for(int i = 1; i < block_data.size(); i++){

			for(int j = 0; j < REGION_SIZE*REGION_SIZE; j++){
				myPix target = new myPix();
				
				target = block_data.get(0).getBlock(j);
				myPix other = block_data.get(i).getBlock(j);
				
				max = Math.max( target.red, other.red );
				min = Math.min( target.red, other.red );
				r = ((max-min)/max)*100;
	
				max = Math.max( target.green, other.green );
                	        min = Math.min( target.green, other.green );
                        	g = ((max-min)/max)*100;
	
				max = Math.max( target.blue, other.blue );
                	        min = Math.min( target.blue, other.blue );
                        	b = ((max-min)/max)*100;

				closenessFactor.get(i).addBlock(new myPix(r,g,b));
				
			}			
		}
		
	} //end of private--close-factor()
	
	private void summation(){
		//sum up all of the block data for each image

		double redSum = 0;
		double greenSum = 0;
		double blueSum = 0;
		myPix temp;
		double sum[] = new double[closenessFactor.size() -1];
		int count = 0;
		
		for(int i = 1; i < closenessFactor.size(); i++){
			
			
			for(int j = 0; j < closenessFactor.get(i).getArraySize(); j++){
				temp = new myPix();
				temp = closenessFactor.get(i).getBlock(j);
				redSum += temp.red;
				greenSum += temp.green;
				blueSum += temp.blue;
				count++;
			}
			double r = redSum / count;
			double g = greenSum / count;
			double b = blueSum / count;
			//System.out.println(r + " "+ g+ " "+ b);
			count = 0;
			
			sum[i-1] = (r+g+b);
			results[i-1] =  new ComparisonResults( (int)(sum[i-1]), files[i]);
			
			//results[i-1].printResult();
			//System.out.println("pic "+ (i+1)+ "= "+sum[i-1]);
		}

	} //end of summation
	
	//function to do summation of all the blocks in closenessFactor---make sure to start at index 0
	//sort out in asending order-- lowest value means more closeness to target image

	private ComparisonResults[] merge_sort(ComparisonResults[] list){
		if(list.length <= 1){
			return list;
		}
		int half = list.length / 2;
		ComparisonResults[] left = new ComparisonResults[half];
		ComparisonResults[] right = new ComparisonResults[half + (list.length % 2)];

		int index = 0;
		for(int i = 0; i < left.length; i++){
			left[i] = list[index++];
		}
		for(int i = 0; i < right.length; i++){
			right[i] = list[index++];
		}

		left = merge_sort(left);
		right = merge_sort(right);

		return merge(left, right);
	} //end of merge_sort

	private ComparisonResults[] merge(ComparisonResults[] left, ComparisonResults[] right){
		ComparisonResults[] retval = new ComparisonResults[left.length + right.length];
		
		ArrayList<ComparisonResults> a_left = new ArrayList<ComparisonResults>();
		ArrayList<ComparisonResults> a_right = new ArrayList<ComparisonResults>();
		
		for(int i = 0; i < left.length; i++){
			a_left.add(left[i]);
		}
		for(int i = 0; i < right.length; i++){
			a_right.add(right[i]);
		}
		int index = 0;
		while(a_left.size() > 0 || a_right.size() > 0){
			if(a_left.size() > 0 && a_right.size() > 0){
				if(a_left.get(0).getClosenessFactor() <= a_right.get(0).getClosenessFactor()){
					retval[index++] = a_left.get(0);
					a_left.remove(0);
				}
				else{
					retval[index++] = a_right.get(0);
					a_right.remove(0);
				}
			}
			else if(a_left.size() > 0){
				while(! a_left.isEmpty() ){
					retval[index++] = a_left.get(0);
					a_left.remove(0);
				}
			}
			else if(a_right.size() > 0){
				while(! a_right.isEmpty()){
					retval[index++] = a_right.get(0);
					a_right.remove(0);
				}
			}		

		} //end of while
		return retval;
	}

	class myPix{
		double red;
		double green;
		double blue;

		public myPix(){
			red = 0;
			green = 0;
			blue = 0;
		}
		public myPix(double r, double g, double b){
			red = r;
            green = g;
            blue = b;
		}
		public void printPix(){
			System.out.println(red + "% "+ green+"%	"+blue+"%");
		}

	}//end of myPix class

	class ImageBlockData{
		private ArrayList<myPix> blocks;

		public ImageBlockData(){ 
			blocks = new ArrayList<myPix>();
		}
		
		public void addBlock(myPix pix){
			//if(blocks.size() >= REGION_SIZE){
			//	return;
			//}
			blocks.add(pix);
		}

		public myPix getBlock(int index){
			return blocks.get(index);
		}
		
		public int getArraySize(){
			return blocks.size();
		}
		
		public ArrayList<myPix> getBlockArray(){
			return blocks;
		}
		
		public void setBlockArray(ArrayList<myPix> arg){
			blocks = arg;
		}
	
		public void printBlocks(DecimalFormat df){
			for(int i = 0; i < blocks.size(); i++){
				System.out.print("block "+ i +":	");
	                        System.out.println( df.format(blocks.get(i).red)+ "% "
        	        	        +df.format(blocks.get(i).green)+ "% "+ df.format(blocks.get(i).blue)+"% ");
			}
		} //end of print
	}//end of ImageBlockData
	
	class ComparisonResults{
		String file;
		int closeness;

		public ComparisonResults(int closenessFactor, String fileName){
			file = new String(fileName);
			closeness = closenessFactor;
		}
		
		public String getFileName(){
			return file;
		}
		
		public void setCloseness(int val){
			closeness = val;
		}

		public int getClosenessFactor(){
			return closeness;
		}

		public void printResult(){
			System.out.println("file: "+ file+ "  closeness factor = "+ closeness);
		}
	} //end of comparisonResults

} //end of class

