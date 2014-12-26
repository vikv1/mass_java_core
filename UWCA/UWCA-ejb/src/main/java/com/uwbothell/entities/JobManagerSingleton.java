/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.uwbothell.entities;

import javax.ejb.Singleton;
import javax.inject.Inject;

/**
 *
 * @author 
 */

@Singleton
public class JobManagerSingleton {
    
  //  @Inject
   // private JobRunner jobRunner;
    public String str = "Universe";
    /*
    private static volatile JobManagerSingleton instance = null;
    
    private JobManagerSingleton(){
        // start runner in the constructor (only gets called once)
    }

    public static synchronized JobManagerSingleton getInstance() {
     //   MassRunner massRunner = new MassRunner();
        if (instance == null){
            instance = new JobManagerSingleton();
        }
        return instance;
    }

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
*/
}
