

import java.io.*;
import java.util.ArrayList;
import java.text.*;
import java.lang.Math;

public class BlockCollection{

//	ArrayList<myPix> block_pic= new ArrayList<myPix>(); //each block's data
//	ArrayList<myPix> block_pic2 = new ArrayList<myPix>(); //each block's data
	ArrayList<ImageBlockData> closenessFactor = new ArrayList<ImageBlockData>(); //closeness factor
	
	ArrayList<Pixel[][]> images = new ArrayList<Pixel[][]>();
	ArrayList<ImageBlockData> block_data;

	//ColorImageScale temp = new ColorImageScale();
	//DecimalFormat df = new DecimalFormat("#.##");

	int delta_x = 0;
	int delta_y = 0;
	final int REGION_SIZE = 10;

	public BlockCollection(ArrayList<Pixel[][]> pics){

		block_data = new ArrayList<ImageBlockData>(pics.size());

		for(int i = 0; i< pics.size(); i++){
			images.add(pics.get(i));
			block_data.add(new ImageBlockData());
			closenessFactor.add(new ImageBlockData());
		} //copy all the images into my local array
	}

	//calculate the percentage of red green and blue for each block
	public void calcBlockPercent(){
		

		for(int i = 0; i < images.size(); i++){ //for each image
			Pixel[][] pic = images.get(i);
			delta_x = pic.length / REGION_SIZE;			
			delta_y = pic[0].length / REGION_SIZE;
			ColorImageScale temp; // = new ColorImageScale();

			for(int x = 0; x < pic.length; x+= delta_x){
				for(int y = 0; y <pic[0].length; y+=delta_y){
					temp = new ColorImageScale();
					temp.setpallet(images.get(i), x, y, delta_x, delta_y);
					block_data.get(i).addBlock(new myPix(temp.getPerRed(), temp.getPerGreen(), temp.getPerBlue()));
				}
			}

		} //image

		//block_data.get(0).printBlocks(new DecimalFormat("#.##"));

		//for(int i = 0; i < block_data.size(); i++){
		//	block_data.get(i).printBlocks(new DecimalFormat("#.##"));	
		//}

		//delta_x = pix.length/REGION_SIZE;
		//delta_y = pix[0].length/REGION_SIZE;


		//for(int i = 0; i < pix.length; i+=delta_x){
		//	for(int j = 0; j < pix[i].length; j+=delta_y){

		//		temp.setpallet(pix, i, j,delta_x, delta_y);
		//		block_pic1.add(new myPix(temp.getPerRed(), temp.getPerGreen(), temp.getPerBlue()));
				
		//	} //inner-loop
		//} //outer

//		for(int i = 0; i < pix_2.length; i+=delta_x){
//                        for(int j = 0; j < pix_2[i].length; j+=delta_y){
//                                temp.setpallet(pix_2, i, j,delta_x, delta_y);
//                                block_pic2.add(new myPix(temp.getPerRed(), temp.getPerGreen(), temp.getPerBlue()));
//
//                        } //inner-loop
//                } //outer

//		 for(int i = 0; i < 10; i++){
//
//			System.out.print("block "+ i +":	");
//                        System.out.println( df.format(block_pic1.get(i).red)+ "% "
//                        +df.format(block_pic1.get(i).green)+ "% "+ df.format(block_pic1.get(i).blue)+"% ");
//			
//			System.out.println("		"+ df.format(block_pic2.get(i).red)+ "% "
  //                      +df.format(block_pic2.get(i).green)+ "% "+ df.format(block_pic2.get(i).blue)+"% ");
    //            }
	
		calculateClosenessFactor();
		summation();

	} //end of calcBlockPercentage


	private void calculateClosenessFactor(){
		//closeness factor
		//double relative = 0;
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
			//closenessFactor.get(i).print();
		
			//System.out.print("block "+ i +":        ");
			//System.out.println("            "+ df.format(closenessFactor.get(i).fac_red)+ "% "
                      	//+df.format(closenessFactor.get(i).fac_green)+ "% "+ df.format(closenessFactor.get(i).fac_blue)+"% ");
		}
		//for(int i = 1; i < closenessFactor.size(); i++){
		//	closenessFactor.get(i).printBlocks(new DecimalFormat("#.##"));	
		//}
		
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
			System.out.println(sum[i-1]);
			//System.out.println("pic "+ (i+1)+ "= "+sum[i-1]);
		}

	} //end of summation
	
	//function to do summation of all the blocks in closenessFactor---make sure to start at index 0

	//sort out in asending order-- lowest value means more closeness to target image


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
			blocks.add(pix);
		}

		public myPix getBlock(int index){
			return blocks.get(index);
		}
		
		public int getArraySize(){
			return blocks.size();
		}
	
		public void printBlocks(DecimalFormat df){
			for(int i = 0; i < blocks.size(); i++){
				System.out.print("block "+ i +":	");
	                        System.out.println( df.format(blocks.get(i).red)+ "% "
        	        	        +df.format(blocks.get(i).green)+ "% "+ df.format(blocks.get(i).blue)+"% ");
			}
		} //end of print

		/*double fac_red;
		double fac_green;
		double fac_blue;
		public CompareImage(){
		}

		public CompareImage(double r, double g, double b){
			fac_red = r;
			fac_green = g;
			fac_blue = b;
		}

		public void print(){
			System.out.println(fac_red+"% "+ fac_green+"% "+ fac_blue+"%"); 
		}

		*/
	}//end of compareImage

} //end of class

