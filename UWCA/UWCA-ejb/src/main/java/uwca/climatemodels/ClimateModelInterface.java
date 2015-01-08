/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uwca.climatemodels;

/**
 *
 * @author jwoodrin
 */
public interface ClimateModelInterface {
    
        /**
     * Gets the file set as a string array containing full paths
     * @return 
     */
    public String[] getFiles();
     
    /**
     * Gets the dimensions of the variable
     * @return 
     */
    public int[][] getDimensions();
    
    public int[][] findYearReadIndexes();
    
    public Object readFullYear(int z, int[][] yearIndices);
    
    public float[] readLocalizedYear(int x, int y, int z);
    
}
