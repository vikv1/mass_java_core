/**
 * Data class. 
 * 
 * @author Yuna GUo
 */
package edu.uw.bothell.css.dsl.MASS;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Data implements Serializable {
    private double[] value;

    //the dimension for data comparison. Default value = -1 
    private int compareDim = -1; 
    
    private int dimension = 0;

    public Data(double[] value, int compareDim) { 
        this.value = value.clone();
        this.compareDim = compareDim;
        this.dimension = value.length;
    }

    public Data(double[] value) {
        this.value = value.clone();
    }

    public Data(Data d, int compareDim) {
        double[] val = d.getValue();
        value = new double[val.length];
        for (int i = 0; i < val.length; i++) {
            value[i] = val[i];
        }
        this.compareDim = compareDim;
    }


    public Data(Data d) {
        this.value = d.getValue().clone();
        this.compareDim = d.getCompareDim();
    }

    public double[] getValue() {
        return value;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dim) {
        dimension = dim;
    }

    public int getCompareDim() {
        return compareDim;
    }

    public void setCompareDim(int dim) {
        this.compareDim = dim;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (double d : value) {
            sb.append(d + ", ");
        }
        sb.append(']');

        sb.append(", compareDim = " + compareDim);
        return sb.toString();
    }
}