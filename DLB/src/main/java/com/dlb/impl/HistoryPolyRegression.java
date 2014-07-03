package main.java.com.dlb.impl;

import main.java.com.dlb.interfaces.DLBBase;
import main.java.com.dlb.polyregress.PolynomialRegression;

/**
 * 
 * @author Bhargav Mistry
 *
 */
public class HistoryPolyRegression implements DLBBase {
	PolynomialRegression polyReg = null;
	double[] polyRegressArr = null;
	
	private static final boolean DEBUG = false;
	
	public HistoryPolyRegression(double[] x, double[] y, int degree) {
		int N = x.length;
		degree += 1;
		
		double[][] mat = new double[degree][degree];
		double[] yzone = new double[degree];
		
		for (int i = 0; i < degree; i++) {
			for (int j = 0; j < degree; j++) {
				mat[i][j] = computeMatrix(i, j, x);
			}
		}
		
		for (int i = 0; i < degree; i++) {
			yzone[i] = computeGaussJordanElimination(i, x, y);
		}
		
		polyRegressArr = doGaussJordanElimination(mat, yzone);
	}
	
	private double[] doGaussJordanElimination(double[][] A, double[] b) {
		
		GaussJordanElimination gaussian = new GaussJordanElimination(A, b);
        if (gaussian.isFeasible()) {
        	if (DEBUG) {
        		System.out.println("Solution to Ax = b");
        	}
            double[] x = gaussian.primal();
            if (DEBUG) {
	            for (int i = 0; i < x.length; i++) {
	            	System.out.printf("%10.6f\n", x[i]);
	            }
            }
            
            return x;
        }
        else {
        	if (DEBUG) {
        		System.out.println("Certificate of infeasibility");
        	}
            double[] y = gaussian.dual();
            if (DEBUG) {
	            for (int j = 0; j < y.length; j++) {
	            	System.out.printf("%10.6f\n", y[j]);
	            }
            }
            return y;
        }
	}
	
	public double computeGaussJordanElimination(int i, double[] x, double[] y) {
		double total = 0.0;
		for (int p = 0; p < x.length; p++) {
			total += (Math.pow(x[p], i) * y[p]);
		}
		
		return total;
	}
	
	private double computeMatrix(int i, int j, double[] x) {
		int N = x.length;
		double total = 0.0;
		
		for (int p = 0; p < N; p++) {
			total += Math.pow(x[p], (i+j));
		}
		return total;
	}
	
	@Override
	public double predict(double xVal) {
		double predictedVal = 0.0;
			for (int i = (polyRegressArr.length - 1); i>=0; i-- ) {
				predictedVal += polyRegressArr[i] * (Math.pow(xVal, i));
			}
		
		return predictedVal;
	}

	@Override
	public double predict(double x1, double y1, double x2, double y2) {
		return 0;
	}

}
