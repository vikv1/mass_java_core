/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.uwbothell.entities;

/**
 *
 * @author jwoodrin
 */
public class ConfigManager {
    
      private static volatile ConfigManager instance = null;
    
    private ConfigManager(){}

    public static synchronized ConfigManager getInstance() {
     //   MassRunner massRunner = new MassRunner();
        if (instance == null){
            instance = new ConfigManager();
        }
        return instance;
    }
    
}
