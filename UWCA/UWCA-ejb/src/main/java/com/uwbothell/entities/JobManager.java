/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.uwbothell.entities;

import javax.ejb.Stateless;

/**
 *
 * @author 
 */
@Stateless
public class JobManager {
    
    private static volatile JobManager instance = null;
    
    private JobManager(){
        // start runner in the constructor (only gets called once)
    }

    public static synchronized JobManager getInstance() {
     //   MassRunner massRunner = new MassRunner();
        if (instance == null){
            instance = new JobManager();
        }
        return instance;
    }

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")

}
