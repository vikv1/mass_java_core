/*

 	MASS Java Software License
	© 2012-2016 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2016 University of Washington. MASS was developed by Computing and Software Systems at University of 
	Washington Bothell.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.

*/

package edu.uw.bothell.css.dsl.MASS.MassData;

import java.io.Serializable;
import java.net.URI;

/**
 * Intended to be expanded once the need for more preferences arises.
 * Currently just holds host and port data in a URI object so the user 
 * does not have to enter it every time.
 * 
 * @author Nicolas
 */
@SuppressWarnings("serial")
public class Preferences implements Serializable
{
    /**
     * The use of URI ensures that host and port data are
     * syntactically correct. Before adding a URI to preferences, the existence 
     * of the host should be confirmed. 
     * 
     */
    private URI uri;
    
    //if user wants auto connection on start, requires mass app to start first
    private boolean autoConnect;
    
    //is user using MASS java or c++ 
    private boolean java;
    
    //directory of MASS program for auto connection
    private String programDirectory;
    
    /**
     * Gets the current URI preferences
     * 
     * @return current URI containing preferred host and port info 
     */
    public URI getURI()
    {
        return uri;
    }
    
    /**
     * Sets the new URI preferences
     * 
     * @param uri the URI containing validated host and port data
     */
    public void setURI(URI uri)
    {
        this.setUri(uri);
    }
    
    public boolean autoConnect()
    {
        return this.isAutoConnect();
    }
    
    public void setAutoConnect(boolean autoConnect)
    {
        this.autoConnect = autoConnect;
    }

    /**
     * @return the uri
     */
    public URI getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(URI uri) {
        this.uri = uri;
    }

    /**
     * @return the autoConnect
     */
    public boolean isAutoConnect() {
        return autoConnect;
    }

    /**
     * @return the java
     */
    public boolean isJava() {
        return java;
    }

    /**
     * @param java the java to set
     */
    public void setJava(boolean java) {
        this.java = java;
    }

    /**
     * @return the programDirectory
     */
    public String getProgramDirectory() {
        return programDirectory;
    }

    /**
     * @param programDirectory the programDirectory to set
     */
    public void setProgramDirectory(String programDirectory) {
        this.programDirectory = programDirectory;
    }
    
}
