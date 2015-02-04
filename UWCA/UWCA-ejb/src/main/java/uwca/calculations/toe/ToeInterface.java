/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uwca.calculations.toe;

import uwca.ProvAdapter;
import uwca.climatemodels.ClimateModelInterface;

/**
 *
 * @author jwoodrin
 */
public interface ToeInterface {
    
    public void executeCalculations();
    
    public int getNumToeYears();
    
    public void setArgs(ClimateModelInterface inputModel, int jobNum, ProvAdapter provLogger);
    
    public void writeNetCdfFiles(String toeRegFile, String toeMinFile, String toeMaxFile);
    
}
