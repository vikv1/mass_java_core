package uwca.calculations.toe.agents;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import edu.uw.bothell.css.dsl.MASS.*;
import static java.lang.StrictMath.abs;
import uwca.calculations.toe.places.TasmaxPlace;
import org.apache.commons.math3.stat.regression.SimpleRegression;

/**
 *
 * @author jwoodrin
 */
public class TasmaxAgent extends Agent{  
    
    // STEP 3 variables
    double climatology = 0;
    
    // STEP 4 variables
    double historicalTolMax = 0;
    double historicalTolMin = 0;

    // STEP 5 variables
    float[] lsrValues = new float[150];    
    double slope;
    double slopeStdError;
   
    /**
     * Methods that are callable from callAll
     */
    public static final int setInitialHistoricalTolerancePosition = 0; // for beginning of step 3 
    public static final int getPlaceIndex = 2;
    public static final int gatherHistoricalTolerance = 3;
    public static final int calculateHistoricalTolerance = 4;
    public static final int setClimatologyInitPosition = 5;
    public static final int gatherClimatologyValues = 6;
    public static final int calculateClimatology = 7;
    public static final int migrateZDimension = 9;
    public static final int gatherLsrValue = 10;
    public static final int calculateLsrValues = 11;
    public static final int getSlopes = 12;
    public static final int getErrorTerm = 13;
    public static final int getClimatologyValue = 14;
    public static final int setInitialLsrPosition = 22;
    public static final int getHistoricalToleranceVals = 23;
    
    /**
     * 
     * @param o 
     */
    public TasmaxAgent(Object o){}
    
    /**
     * Call All methods
     * @param funcId
     * @param o
     * @return 
     */
    public Object callMethod( int funcId, Object o ) {
        switch ( funcId ) {
         
            case getPlaceIndex:
                return getPlaceIndex(o);    
             case setInitialHistoricalTolerancePosition:
                 return setInitialHistoricalTolerancePosition(o);
             case gatherHistoricalTolerance:
                 return gatherHistoricalTolerance(o);
             case calculateHistoricalTolerance:
                 return calculateHistoricalTolerance(o);
             case setClimatologyInitPosition:    
                 return setClimatologyInitPosition(o);
             case gatherClimatologyValues:
                 return gatherClimatologyValues(o);
             case calculateClimatology:
                 return calculateClimatology(o);
             case migrateZDimension:
                 return migrateZDimension(o);
             case gatherLsrValue:
                 return gatherLsrValue(o);
             case calculateLsrValues:
                 return calculateLsrValues(o);
             case getSlopes:
                 return getSlopes(o);
             case getErrorTerm:
                 return getErrorTerm(o);
             case getClimatologyValue:
                 return getClimatologyValue(o);
             case setInitialLsrPosition:
                 return setInitialLsrPosition(o);
             case getHistoricalToleranceVals:
                 return getHistoricalToleranceVals(o);
            default:
                return null;
        }
    }
    
    /******************************************************************************************************************
     * STEP 2 methods: Find Historical Tolerance
     * @param o
     * @return 
     *****************************************************************************************************************/
    /**
     * Decides the initial position of an agent
     * Designed to be re-usable for several agent calculations
     * @param o
     * @return 
     */
    public Object setInitialHistoricalTolerancePosition(Object o){
        int ind[] = (int[])o;
        
        int xModifier = this.getAgentId() % ind[0];
        int yModifier = this.getAgentId() / ind[0];
        
        migrate(xModifier, yModifier, 0);
        
        return null;
    }
    
    /**
     * hops from place to place in the z axis (year) finding the min / max values
     * @param o
     * @return 
     */
    public Object gatherHistoricalTolerance(Object o){
        int x = (Integer)this.getPlace().callMethod(TasmaxPlace.getDaysOverThreshold, o);
        if(historicalTolMax < x){
            historicalTolMax = x;
        }
        if(historicalTolMin > x){
            historicalTolMin = x;
        }   
        int xModifier = this.getPlace().getIndex()[0];
        int yModifier = this.getPlace().getIndex()[1];
        int zModifier = this.getPlace().getIndex()[2];
        zModifier++;
        
        migrate(xModifier, yModifier, zModifier);
        return null;
    }
 
    /**
     * Part of STEP 2: Performs the calculation to find the historical min and max values
     * @param o
     * @return 
     */
    public Object calculateHistoricalTolerance(Object o){
        double minMax = (double)o;
        double temp1 = historicalTolMax + abs(historicalTolMin);
        double temp2 = temp1 * minMax;
        historicalTolMax = historicalTolMax - temp2;        
        historicalTolMin = historicalTolMin + temp2;
        return null;
    }
    
    /**
     * Part of STEP 2: Returns the high and low historical tolerance values
     * @param o
     * @return 
     */
    public Object getHistoricalToleranceVals(Object o){
    
        double[] histTolVals = new double[] {historicalTolMin 
            ,historicalTolMax
            ,this.getPlace().getIndex()[0]
            ,this.getPlace().getIndex()[1]
            };
        return histTolVals;
    }
    
    /******************************************************************************************************************
     * STEP 3 methods: Find Climatology
     * @param o
     * @return 
     *****************************************************************************************************************/
    
    /**
     * STEP 3: Sets the initial position of the Agent to start performing Climatology analysis
     * @param o
     * @return 
     */
    public Object setClimatologyInitPosition(Object o){            
      
        int xModifier = this.getPlace().getIndex()[0];
        int yModifier = this.getPlace().getIndex()[1];
        int zModifier = (int)o;
        
        migrate(xModifier, yModifier, zModifier);        
        return null;
    }
    
    /**
     * STEP 3: Gathers the daysOverThreshold value from the place, and migrates once in the z (time) dimension
     * @param o
     * @return 
     */
    public Object gatherClimatologyValues(Object o){
        int xModifier = this.getPlace().getIndex()[0];
        int yModifier = this.getPlace().getIndex()[1];
        int zModifier = this.getPlace().getIndex()[2];
        zModifier++;
        climatology = climatology + (Double)this.getPlace().callMethod(TasmaxPlace.getDaysOverThreshold, o);
        migrate(xModifier, yModifier, zModifier);        
        return null;
    }
    
    /**
     * STEP 3: Calculate the climatology (happens after all values have been gathered)
     * @param o
     * @return 
     */
    public Object calculateClimatology(Object o){
        climatology = climatology / 30;
        return null;
    }
    
    /**
     * STEP 3: returns the climatology value
     * @param o
     * @return 
     */
    public Object getClimatologyValue(Object o){        
        return this.climatology;
    }
    
    /******************************************************************************************************************
     * STEP 4 methods: Gathers the daysOverThreshold variable from the place where the Agent currently resides
     * @param o
     * @return 
     *****************************************************************************************************************/
    
    /**
     * Part of STEP 4: Sets the initial position for the LSR calculations
     * @param o
     * @return 
     */
    public Object setInitialLsrPosition(Object o){
        int xIndex = this.getPlace().getIndex()[0];
        int yIndex = this.getPlace().getIndex()[1];
        int zIndex = (int)o;
        
        migrate(xIndex, yIndex, zIndex);
        return null;
    }
    
    /**
     * Part of STEP 4
     * @param o
     * @return 
     */
    public Object getErrorTerm(Object o){
    
        return slopeStdError;
    }
    
    /**
     * part of STEP 4: returns the calculated slope value
     * @param o
     * @return 
     */
    public Object getSlopes(Object o){    
        return slope;
    }
    
    /**
     * part of STEP 4: returns the lsrValues
     * @param o
     * @return 
     */
    public Object calculateLsrValues(Object o){
        
        SimpleRegression regression = new SimpleRegression();        
        int year = 1950;
        for(int i = 0; i < lsrValues.length; i++){
            regression.addData(lsrValues[i], year);
            year++;
        }
        
        slope = regression.getSlope();
        slopeStdError = regression.getSlopeStdErr(); 
        return null;       
    }
    
    /**
     * part of STEP 4: gathers individual daysOverThreshold value from the place the agent resides on
     * @param o
     * @return 
     */
    public Object gatherLsrValue(Object o){
        int index = this.getPlace().getIndex()[2];
        lsrValues[index] = (Integer)this.getPlace().callMethod(TasmaxPlace.getDaysOverThreshold, o);
        return null;
    }
    
    /**
     * part of STEP 4: migrates one element in the z dimension
     * @param o
     * @return 
     */
    public Object migrateZDimension(Object o){
        int xModifier = this.getPlace().getIndex()[0];
        int yModifier = this.getPlace().getIndex()[1];
        int zModifier = this.getPlace().getIndex()[2];
        zModifier++;
        migrate(xModifier, yModifier, zModifier);
        return null;
    }    

    /**
     * Returns the index array showing the place location that the agent currently resides on
     * @param o
     * @return 
     */
    public Object getPlaceIndex(Object o){
        // return the place index
        return (Object)this.getPlace().getIndex();
    }    
}
