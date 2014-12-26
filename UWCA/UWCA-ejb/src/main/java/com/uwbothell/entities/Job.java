/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.uwbothell.entities;

import analytics.ManagementVar;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jwoodrin
 */
public class Job {
    // the management variable we'll be calculating
    private String var;

    
    private String varName;

    public String getVarName() {
        return varName;
    }

    public void setVarName(String varName) {
        this.varName = varName;
    }
    // the data models to use
    List<String> dataModels;
    
    private String dataModel;

    public String getDataModel() {
        return dataModel;
    }

    public void setDataModel(String dataModel) {
        this.dataModel = dataModel;
    }
    
    private String status;

    public String getVar() {
        return var;
    }

    public List<String> getDataModels() {
        return dataModels;
    }

    public String getStatus() {
        return status;
    }

    public void setDataModels(List<String> dataModels) {
        this.dataModels = dataModels;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
    public Job(){
        dataModels = new ArrayList();
    }
    
    public void setVar(String var) {
        this.var = var;
    }    

}
