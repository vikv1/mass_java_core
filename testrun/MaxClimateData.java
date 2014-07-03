import java.io.Serializable;

// MaxClimateData.java  -- Cherie Wasous 3.5.2014

// public class MaxClimateData { // OK if no slave nodes
public class MaxClimateData implements Serializable {

	public double mcdFlux;
	public double mcdDir;
	public int mcdTime, mcdDay;
	public int mcdX, mcdY;

	// Constructors
	// ------------

	public MaxClimateData() {
		super();
	}

	public MaxClimateData(double flux, double dir, int time, int day, int x,
			int y) {
		super();
		mcdFlux = flux;
		mcdDir = dir;
		mcdTime = time;
		mcdDay = day;
		mcdX = x;
		mcdY = y;

	}
}
