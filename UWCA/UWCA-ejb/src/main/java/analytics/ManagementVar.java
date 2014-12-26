/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package analytics;

import com.uwbothell.entities.StatusAdapter;

/**
 *
 * @author jwoodrin
 */
public interface ManagementVar {
    // required method for all classes extending Place
    public Object callMethod(int funcId, Object args);
  
}
