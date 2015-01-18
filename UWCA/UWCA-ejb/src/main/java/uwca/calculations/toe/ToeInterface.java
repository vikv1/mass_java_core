/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uwca.calculations.toe;

import edu.uw.bothell.css.dsl.MASS.Agents;
import edu.uw.bothell.css.dsl.MASS.Places;
import uwca.climatemodels.ClimateModelInterface;

/**
 *
 * @author jwoodrin
 */
public interface ToeInterface {
    
    public void executeCalculations();
    
    public void setArgs(ClimateModelInterface inputModel, int jobNum);
    
}
