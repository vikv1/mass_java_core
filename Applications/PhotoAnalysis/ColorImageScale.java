

/************ general logic ********************
 - read each red, green, and blue pixel in their corresponding array
 - calculate the percentage of the array of each value with total
 

*******************************************/

import java.text.*;

public class ColorImageScale{
	
	int[] red;
	int[] green;
	int[] blue;

	int total_red;
	int total_green;
	int total_blue;

	int total_pix;
	DecimalFormat df;

	double perRed;
	double perGreen;
	double perBlue;

	public ColorImageScale(){ 

		//cause values can go up to 255 (so 1 more than that and ignore index 0)
		//cause values can go up to 255 (so 1 more than that and 
		//ignore index 0)
		 reset();
	}

	protected void reset(){
		df = new DecimalFormat("#.##");

                red = new int[256];          //think 0 will be ignored and need 255
                green = new int[256];
                blue = new int[256];

                total_pix = 0;

                total_red = 0;
                total_green = 0;
                total_blue = 0;

                perRed = 0;
                perGreen = 0;
                perBlue = 0;

	}
	
	//calculate the rgb of a specific area of an image
	public void setpallet(Pixel[][] pixels,int start_x, int start_y, int delta_x, int delta_y){
		//take input from file and populate

		//*********************************
		reset();
               	//***************************

		//total_pix = (pixels.length * pixels[0].length);
		int del_x = delta_x;
		int del_y = delta_y;
		total_pix = 0; //particular area of picture
		int endx = start_x + delta_x;
		int endy = start_y + delta_y;
				
		if( (pixels.length + endx) < delta_x){
			endx += (pixels.length - endx);
			del_x += (pixels.length - endx);
		}
		if( (pixels[0].length + endy) < delta_y){
			endy += (pixels[0].length - endy);
			del_y += (pixels[0].length - endy);
		}
			
		total_pix = del_x * del_y;

		//only do the first 100 pixels
		//count number of each value occurance
		if(endx >= pixels.length){
			endx = pixels.length - 1;
		}
		if(endy >= pixels[0].length){
			endy = pixels[0].length - 1;
		}

		for(int i = start_x; i < endx; i++){
			for(int j = start_y; j < endy; j++){
				red[pixels[i][j].r] += 1; //increment count
				green[pixels[i][j].g] += 1;
				blue[pixels[i][j].b] += 1;

			} //inner loop
		} // outer loop		

		//calculate the total number of red, green, blue pixels
		for(int i = 1; i < 256; i++){

			total_red += red[i]*i;
			total_green += green[i]*i;
			total_blue += blue[i]*i;			

		} //count total red,green, blue pixels
		
		perRed = ((double)total_red / (double)total_pix);
		perGreen = ((double)total_green / (double)total_pix);
		perBlue = ((double)total_blue / (double)total_pix);

	} //end of setpallet

	public double getPerRed(){
		return (perRed/3);
	}

	public double getPerGreen(){
		//if(perGreen < 0) return 0;
		return (perGreen/3);
	}

	public double getPerBlue(){
		return (perBlue/3);
	}

	public void printColorPercentage(){
		 System.out.println("red = "+ df.format(((double)total_red / (double)total_pix))+ " %");
		 System.out.println("green = "+ df.format(((double)total_green / (double)total_pix))+ " %");
		 System.out.println("blue = "+ df.format(((double)total_blue / (double)total_pix))+ " %");
	}

} //end of class
