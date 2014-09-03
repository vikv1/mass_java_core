/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.uwbothell.entities;

import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.Place;
import edu.uw.bothell.css.dsl.MASS.Places;
import java.util.Vector;

/**
 *
 * @author jwoodrin
 */
public class MassRunner extends Place{
    private int x;
    int interval = 0;
    public static final int printInt = 0;
    public static final int modifyInt = 1;
    public static final int initializeInt = 2;
    public static final int exchangeInt = 3;
    public static final int readInt = 4;
    
    public int[] myPlace;
    public MassRunner(){
        

    }    
    public String run(){
            //  this.interval = ( ( Integer)interval ).intValue();
        
        String[] massArgs = new String[4];
        int numProc = 1;
        int numThr = 2;
        massArgs[0] = "nonsense";
        massArgs[1] = "alsoNonsense";
        massArgs[2] = "machinefile.txt";
        massArgs[3] = "15454";
        MASS.init(massArgs, numProc, numThr);
        int interv = 0;
        
        Vector<int[]> notNeighbors = new Vector<int[]>( );
        int[] east = { 2, 0 }; notNeighbors.add(east);
        int[] west = { -2, 0 }; notNeighbors.add(west); 
        
        Places places = new Places(1, "massintro.MassIntro", (Object)(new Integer(interv)), 10, 10);

        places.callAll(initializeInt);
        System.out.println("*******************************************************");
        System.out.println("Printing Initial Int");
        places.callAll(printInt);
        places.callAll(modifyInt);
        System.out.println("*******************************************************");
        System.out.println("Printing modified Int");
        places.callAll(printInt);
        
        places.exchangeAll(1, exchangeInt, notNeighbors);
        places.callAll(readInt);
        System.out.println("*******************************************************");
        System.out.println("Printing exchanged Int");
        places.callAll(printInt);
        MASS.finish();
        
        return "true";
    
    }
    /*
     public MassIntro(Object interval){
        this.interval = ( ( Integer)interval ).intValue();
    }
    */
    public Object callMethod(int i, Object o){
        switch(i){
            case printInt:                
                return PrintInt(o);   
            case modifyInt:                
                return ModifyInt(o); 
            case initializeInt:                
                return InitializeInt(o); 
            case exchangeInt:                
                return ExchangeInt(o); 
            case readInt:                
                return ReadInt(o); 
            default:
                return null;              
        }
    }
    public Object PrintInt(Object o){
  //      System.out.println(x);
        System.out.println(myPlace[0] + ", " + myPlace[1] + ", x: " + x);
        return null;
    }
    public Object ModifyInt(Object o){
        x = (x + 1) * x;
        if(myPlace[0] == 5){
            x = x * 10;
        }
        return null;
    }
    public Object InitializeInt(Object o){
        x = 1;
        myPlace = index;  // index is a global variable in MASS
        return null;
    }
    
    public int ExchangeInt( Object args ) {        
        return (int) x; // may need to be in Integer format
    }
    
    public Object ReadInt( Object args){
        for(int i = 0; i<2; i++){
            if(inMessages[i] != null)
               x += (Integer) inMessages[i];
            else
                System.err.println("found a null in: " + myPlace[0] + ", " + myPlace[1]);
        }
        x /= 3;
        return null;
    }
}
