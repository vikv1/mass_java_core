/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uwca;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

/**
 *
 * This class is responsible for logging calculation events to the log file for the job being executed
 */
public class ProvAdapter {
    
    private String provFile;
    
    public ProvAdapter(String fileName){
        provFile = fileName;
    }
    
    /**
     * Logs the specified message to the provenance file
     * @param message 
     */
    public void logProvenance(String message){
        
        try
        {         
            FileWriter fw = new FileWriter(provFile,true);  //the true will append the new data
            fw.write(message + "\n");                       //appends the string to the file
            fw.close();
        }
        catch(IOException ioe)
        {
            System.err.println("IOException: " + ioe.getMessage());
        }
    }
    
}
