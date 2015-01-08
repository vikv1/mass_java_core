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
/*

*/
public class TasmaxAgent extends Agent{
    
    public static final int decideNewPosition_ = 8;
    public static final int decideInitialPosition = 0;
    public static final int decideNewPosition = 1;
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
    public static final int getConInterval = 13;
    public static final int getClimatologyValue = 14;
    
    float[] lsrValues = new float[150];
    
    double slope;
    double slopeStdError;
    
    double historicalTolMax = 0;
    double historicalTolMin = 0;
    double climatology = 0;
    
    public TasmaxAgent(Object o){}
    
    public Object callMethod( int funcId, Object o ) {
        switch ( funcId ) {
            case decideNewPosition_: 
                return decideNewPosition( o );
            case getPlaceIndex:
                return getPlaceIndex(o);
             case decideNewPosition:
                return decideNewPosition(o);
             case decideInitialPosition:
                 return decideInitialPosition(o);
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
             case getConInterval:
                 return getConInterval(o);
             case getClimatologyValue:
                 return getClimatologyValue(o);
            default:
                return null;
        }     
    }
    
    /**
     * 
     * @param o
     * @return 
     */
    public Object getClimatologyValue(Object o){
        return this.climatology;
    }
    
    /**
     * Part of STEP 5
     * @param o
     * @return 
     */
    public Object getConInterval(Object o){
    
        return slopeStdError;
    }
    
    /**
     * part of STEP 5
     * @param o
     * @return 
     */
    public Object getSlopes(Object o){    
        return slope;
    }
    
    /**
     * returns the lsrValues for STEP 5
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
     * part of STEP 5
     * @param o
     * @return 
     */
    public Object gatherLsrValue(Object o){
        int index = this.getPlace().getIndex()[2];
        lsrValues[index] = (Integer)this.getPlace().callMethod(TasmaxPlace.returnInt, o);
        return null;
    }
    
    /**
     * method to gather array information for STEP 5
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
     * for STEP 4
     * @param o
     * @return 
     */
    public Object calculateClimatology(Object o){
        climatology = climatology / 30;
        return null;
    }
    
    public Object gatherClimatologyValues(Object o){
        int xModifier = this.getPlace().getIndex()[0];
        int yModifier = this.getPlace().getIndex()[1];
        int zModifier = this.getPlace().getIndex()[2];
        zModifier++;
        climatology = climatology + (Double)this.getPlace().callMethod(TasmaxPlace.returnInt, o);
        migrate(xModifier, yModifier, zModifier);
        
        return null;
    }
    
        public Object setClimatologyInitPosition(Object o){
        
        int xModifier = this.getPlace().getIndex()[0];
        int yModifier = this.getPlace().getIndex()[1];
        int zModifier = 30;
        
        migrate(xModifier, yModifier, zModifier);
        
        return null;
    }
    
    public Object calculateHistoricalTolerance(Object o){
        
        double temp1 = historicalTolMax + abs(historicalTolMin);
        double temp2 = temp1 * 0.10;
        historicalTolMax = historicalTolMax - temp2;
        
        historicalTolMin = historicalTolMin + temp2;
        return null;
    }
    
        /**
     * hops from place to place in the z axis (year) finding the min / max values
     * @param o
     * @return 
     */
    public Object gatherHistoricalTolerance(Object o){
        int x = (Integer)this.getPlace().callMethod(TasmaxPlace.returnInt, o);
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
    
        public Object decideNewPosition( Object args ){
        int agent_id = this.getAgentId();
        int xModifier = this.getPlace().getIndex()[0];
        int yModifier = this.getPlace().getIndex()[1];
        int zModifier = this.getPlace().getIndex()[2];
        zModifier++;
        
        migrate(xModifier, yModifier, zModifier);
        return null;
    }
    
        /**
     * Decides the initial position of an agent
     * @param o
     * @return 
     */
    public Object decideInitialPosition(Object o){
        int ind[] = (int[])o;
        
        int xModifier = this.getAgentId() % ind[0];
        int yModifier = this.getAgentId() / ind[0];
        
        migrate(xModifier, yModifier, 0);
        
        return null;
    }
        /**
     * 
     * @param o
     * @return 
     */
    public Object getPlaceIndex(Object o){
        
        return (Object)this.getPlace().getIndex();
    }
    
}
