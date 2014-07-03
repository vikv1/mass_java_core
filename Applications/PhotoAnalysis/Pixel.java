public class Pixel{

	public int r,g,b;
	private final double cRed = .299;
	private final double cGreen = .587;
	private final double cBlue = .114;


	Pixel(int yInt, int cbInt, int crInt){
		double y, cb, cr;
		y = (double)(yInt);
		if(yInt > 127){

			////System.out.println("yInt: " + yInt);
			yInt = 127;
		}
		if(yInt < -127){

			////System.out.println("yInt: " + yInt);
			yInt = -127;
		}
		cb = (double)(cbInt);
		if(cbInt > 127 || cbInt < -127){
			////System.out.println("cbInt: " + cbInt);
		}
		cr = (double)(crInt);
		if(crInt > 127 || crInt < -127){
			//	//System.out.println("crInt: " + crInt);
		}
		
		
		double floatR = (int)(cr * (2 - (2 * .299)) + y);
		double floatB = (int)(cb * (2 - (2 * .114)) + y);
		double floatG = (int)((y - (.114 * floatB) - (.299 * floatR)) / .587);
		
		r = (int)(floatR);
		b = (int)(floatB);
		g = (int)(floatG);
		
		r += 128;
		g += 128;
		b += 128;
		
		if(r < 0){
			////System.out.println("RED < 0, " + r);
			r = 0;
		}
		
		if(r > 255){
			////System.out.println("RED > 255, " + r);
			r = 255;
		}
		if(g < 0){
			////System.out.println("GREEN < 0, " + g);
			g = 0;
		}
		if(g > 255){
		//	//System.out.println("GREEN > 255, " + g);
			g = 255;
		}
		if(b < 0){
		//	//System.out.println("BLUE < 0, " + b);
			b = 0;
		}
		if(b > 255){
		//	//System.out.println("BLUE > 255, " + b);
			b = 255;
		}
	}

	//print each pixel's value: in red, green, blue format
	public void printPixel(){
		System.out.println("Red = "+ r + "  Green = "+ g +"  Blue = "+ b);		
	}
}
