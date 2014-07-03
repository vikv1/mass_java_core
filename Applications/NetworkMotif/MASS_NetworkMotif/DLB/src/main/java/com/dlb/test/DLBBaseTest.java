package main.java.com.dlb.test;

import main.java.com.dlb.impl.HistoryPolyRegression;
import main.java.com.dlb.impl.SlopeBasedAlgo;
import main.java.com.dlb.interfaces.DLBBase;

import org.junit.Test;

public class DLBBaseTest {
	
	//@Test
	public void predictFullHistoryTest() {
		double[] x = {-1, 0, 1, 2, 3, 5, 7, 9};
		double[] y = {-1, 3, 2.5, 5, 4, 2, 5, 4};
		int degree = 4;
		DLBBase dlbBase = new HistoryPolyRegression(x, y, degree);
		double preVal = dlbBase.predict(9.5);
		System.out.println("@Test predictFullHistoryTest : " + preVal);
	}
	
	@Test
	public void predictWindowTest() {
		double[] x = {5, 7, 9};
		double[] y = {2, 5, 4};
		
		int degree = 4;
		DLBBase dlbBase = new HistoryPolyRegression(x, y, degree);
		System.out.println("@Test predictWindowTest : " + dlbBase.predict(9.5));
	}
	
	//@Test
	public void predictSlopeTest() {
		DLBBase dlbBase = new SlopeBasedAlgo();
		
		System.out.println("Predicted slope : " + dlbBase.predict(5, 2, 7, 5));
	}
	
	//@Test
	public void testPredictionFullHistory() {
				
		//double[] x = {0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95};
		//double[] y = { 1, 3, 2.5, 5, 4, 2, 0, 4, 3, 3.5, 4, 4, 3.2, 1, 1.6, 0, 5, 6.3, 4, 9};
		
		double[] x = {0, 5, 10, 15, 20, 25, 30, 35, 40, 45};
		double[] y = {1, 3, 2.5, 5, 4, 2, 0, 4, 3, 3.5};
		
		double[] holderX = new double[20];
		double[] holderY = new double[20];
		for (int i = 0; i < y.length; i++) {
			holderX[i] = x[i];
			holderY[i] = y[i];
		}
		DLBBase dlbBase = null;
		int time = 45;
		for (int i = 10; i < 20; i++) {
			dlbBase = new HistoryPolyRegression(holderX, holderY, 4);
			holderX[i] = time+=5;
			holderY[i] = dlbBase.predict(time);
		}
		
		for (int i = 0; i < 20; i++) {
			System.out.printf("%10.2f ", holderX[i]);
			
		}
		
		System.out.println("now printing Y.............");
		
		for (int i = 0; i < 20; i++) {
			System.out.printf("%10.2f ", holderY[i]);
		}
	}
	
	//@Test
	public void testWindowPrediction() {
//		double[] x = {0, 5, 10, 15, 20, 25, 30, 35, 40, 45};
//		double[] y = {1, 3, 2.5, 5, 4, 2, 0, 4, 3, 3.5};
		
		double[] x = {0, 5, 10};
		double[] y = {1, 3, 2.5};
		
		double[] holderX = new double[20];
		double[] holderY = new double[20];
		double[] interX = new double[3];
		double[] interY = new double[3];
		for (int i = 0; i < y.length; i++) {
			holderX[i] = x[i];
			holderY[i] = y[i];
			interX[i] = x[i];
			interY[i] = y[i];
		}
		
		DLBBase dlbBase = null;
		int time = 10;
		for (int i = 3; i < 20; i++) {
			dlbBase = new HistoryPolyRegression(interX, interY, 4);
			interX[0] = interX[1];
			interX[1] = interX[2];
			interX[2] = time+=5;
			
			holderX[i] = time;
			
			interY[0] = interY[1];
			interY[1] = interY[2];
			interY[2] = dlbBase.predict(time);
			holderY[i] = interY[2];
		}
		
		for (int i = 0; i < 20; i++) {
			System.out.printf("%10.2f ", holderX[i]);
			
		}
		
		System.out.println();
		System.out.println("now printing Y.............");
		
		for (int i = 0; i < 20; i++) {
			System.out.printf("%10.2f ", holderY[i]);
		}
	}
	
	//@Test
	public void testFullHistoryErrorRate() {
		double[] x = {0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95};
		double[] y = { 1, 3, 2.5, 5, 4, 2, 0, 4, 3, 3.5, 4, 4, 3.2, 1, 1.6, 0, 5, 6.3, 4, 9};
		
		double[] interX = new double[20];
		double[] interY = new double[20];
		
		for (int i = 0; i < 10; i++) {
			interX[i] = x[i];
			interY[i] = y[i];
		}
		
		DLBBase dlbBase = null;
		double predictedY = 0.0;
		double percentage = 0.0;
		int count = 0;
		for (int i = 10; i < x.length; i++) {
			interX[i] = x[i];
			interY[i] = y[i];
			dlbBase = new HistoryPolyRegression(interX, interY, 4);

			if (i < x.length-1) {
				predictedY = dlbBase.predict(x[i+1]);
				double diff = Math.abs(predictedY - y[i+1]);
				double greaterNum = (predictedY > y[i+1]) ? predictedY : y[i+1];
				if (greaterNum != 0) {
					percentage = ((diff/greaterNum) * 100);
					count++;
					System.out.println("Values diff and greaterNum : ["+diff+"] greaterNum ["+greaterNum+"] percentage ["+percentage+"]");
					System.out.println("Predicted val and actual val : ["+predictedY+"] actual val : ["+y[i+1]+"]");
				}
				
			}
		}
				
	}
	
	//@Test
	public void testWindowErrorRate() {
		double[] x = {0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95};
		double[] y = { 1, 3, 2.5, 5, 4, 2, 0, 4, 3, 3.5, 4, 4, 3.2, 1, 1.6, 0, 5, 6.3, 4, 9};
		
		double[] interX = new double[3];
		double[] interY = new double[3];
		
		for (int i = 0; i < 3; i++) {
			interX[i] = x[i];
			interY[i] = y[i];
		}
		
		DLBBase dlbBase = null;
		double predictedY = 0.0;
		double percentage = 0.0;
		
		for (int i = 3; i < x.length; i++) {
			
			dlbBase = new HistoryPolyRegression(interX, interY, 4);
			
			if (i < x.length-1) {
				predictedY = dlbBase.predict(x[i+1]);
				double diff = Math.abs(predictedY - y[i+1]);
				double greaterNum = (predictedY > y[i+1]) ? predictedY : y[i+1];
				if (greaterNum != 0) {
					percentage = ((diff/greaterNum) * 100);
					System.out.println("Values diff and greaterNum : ["+diff+"] greaterNum ["+greaterNum+"] percentage ["+percentage+"]");
					System.out.println("Predicted val and actual val : ["+predictedY+"] actual val : ["+y[i+1]+"]");
				}
			}
			
			interX[0] = interX[1];
			interX[1] = interX[2];
			interX[2] = x[i];
			
			interY[0] = interY[1];
			interY[1] = interY[2]; 
			interY[2] = y[i];
		}
	}
	
	//@Test
	public void testRest10Predictions() {
		double[] x = {0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95};
		double[] y = { 1, 3, 2.5, 5, 4, 2, 0, 4, 3, 3.5, 4, 4, 3.2, 1, 1.6, 0, 5, 6.3, 4, 9};
		
		double[] interX = new double[20];
		double[] interY = new double[20];
		
		for (int i = 0; i < 10; i++) {
			interX[i] = x[i];
			interY[i] = y[i];
		}
		DLBBase dlbBase = null;
		for (int i = 10; i < x.length; i++) {
			dlbBase = new HistoryPolyRegression(interX, interY, 4);
			System.out.println("Predicted y for x : x ["+x[i]+"] predicted y : ["+dlbBase.predict(x[i])+"]");
			interX[i] = x[i];
			interY[i] = y[i];
		}
	}
	
	//@Test
	public void test10PredictionsWindowbased() {
		double[] x = {0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95};
		double[] y = { 1, 3, 2.5, 5, 4, 2, 0, 4, 3, 3.5, 4, 4, 3.2, 1, 1.6, 0, 5, 6.3, 4, 9};
		
		double[] interX = new double[3];
		double[] interY = new double[3];
		
		interX[0] = x[0];
		interX[1] = x[1];
		interX[2] = x[2];
		
		interY[0] = y[0];
		interY[1] = y[1];
		interY[2] = y[2];
		DLBBase dlbBase = null;
		for (int i = 3; i<x.length; i++) {
			dlbBase = new HistoryPolyRegression(interX, interY, 4);
			System.out.println("Prediction y for x : ["+x[i]+"] predicted y ["+dlbBase.predict(x[i])+"]" );
			
			interX[0] = interX[1];
			interX[1] = interX[2];
			interX[2] = x[i];
			
			interY[0] = interY[1];
			interY[1] = interY[2];
			interY[2] = y[i];
		}
	}
}
