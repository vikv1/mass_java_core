/*

 	MASS Java Software License
	© 2012-2020 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2020 University of Washington. MASS was developed by Computing and Software Systems at University of 
	Washington Bothell.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.

*/

package edu.uw.bothell.css.dsl.MASS.factory;

public interface ObjectFactory {

	/**
	 * Add a URI to the list of resource paths available to this factory
	 * @param uri The URI to add to the list of available URLs, as a String
	 * @throws Exception If the specified URI is not valid or suitable for use by the factory
	 */
	public void addUri(String uri) throws Exception;
	
	/**
	 * Add a library ("Jar") to the list of resource paths available to this factory
	 * @param libraryName The filename (including directory spec if not in current working directory)
	 * @throws Exception If the specified library is not valid or suitable for use by the factory
	 */
	public void addLibrary(String libraryName) throws Exception;
	
	/**
	 * Get a new, initialized, instance of a specified class
	 * @param className The name of the class from which to create a new instance
	 * @param constructorArgument An Object supplied to the new instance constructor
	 * @return A new initialized instance of the class
	 * @throws Exception If any exception occurs during instantiation of the new object
	 */
	public <T> T getInstance(String className, Object constructorArgument) throws Exception;

}
