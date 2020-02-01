/*

 	MASS Java Software License
	© 2012-2015 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2015 University of Washington. MASS was developed by Computing and Software Systems at University of 
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

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class SimpleObjectFactory implements ObjectFactory {

	public static ObjectFactory getInstance() {
		return INSTANCE;
	}
	
	// this makes the SimpleObjectFactory a singleton, eagerly initialized
	// we may want to lazily create/init object factories if multiple types are available
	private static final SimpleObjectFactory INSTANCE = new SimpleObjectFactory();
	
	// a collection of the URLs to be used by the classloader
	private List<URL> classpathUrls = new ArrayList<URL>();

	// the classloader instance used by this factory
    private URLClassLoader classLoader;
	
	private SimpleObjectFactory() {
		
		// by default, use the current directory as a resource for the classloader
		try {
			classpathUrls.add((new File(".")).toURI().toURL());
		} catch (Exception e) {
			// TODO should this really be swallowed?
		}
		
		// initialize the classloader
		initClassLoader();
		
	}

	@Override
	public void addLibrary(String libraryName) throws Exception {
		addUri("jar:file:" + libraryName + "!/");
	}

	@Override
	public void addUri(String url) throws Exception {

		// add the URL to the collection
		try {
			classpathUrls.add(new URL(url));
		}
		
		catch (MalformedURLException e) {
			
			// the URL specified is not valid
			throw new Exception("The URL specified as a classpath source is invalid", e);
			
		}
		
		// necessary to re-init the classloader using the updated collection of URLs
		initClassLoader();
		
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getInstance(String className, Object constructorArgument) throws Exception {

		Class<T> newClass = null;
		Constructor<T> newClassConstructor = null;

		// first, try using the current classloader to get the class
		try {
			
			newClass = (Class<T>) Class.forName(className, true, Thread.currentThread().getContextClassLoader()); 
		
		}
		catch (ClassNotFoundException e) {

			// not found, try using the local classloader to get the class
			try {
				
				newClass = (Class<T>) Class.forName( className, true, classLoader ); 

			}
			catch (ClassNotFoundException cnfe) {
				
				// class could not be found using either method - fatal
				throw new Exception("Unable to find " + className, cnfe);
			
			}

		}

		try {
			newClassConstructor = newClass.getConstructor( Object.class );
		}
		catch (NoSuchMethodException e) {
			throw new Exception("Class " + className + " requires a constructor accepting an Object as an argument", e);
		}
		catch (SecurityException e) {
			throw new Exception("Class " + className + " lacks the necessary privileges to execute in this environment", e);
		}
	
		T newObjectInstance = null;
		try {
			newObjectInstance =  newClassConstructor.newInstance( constructorArgument );
		}
		catch (InvocationTargetException e) {
			throw new Exception("Exception occurred during instantiation of " + className + ": " + e.getCause(), e.getCause());
		} catch (Exception e) {
			throw new Exception("Exception occurred during instantiatoin of " + className + ": " + e.getCause(), e.getCause());
		}
		
		return newObjectInstance;
	
	}
	
	/**
	 * Set the classloader used by this factory, using the URLs previously set
	 */
	private void initClassLoader() {
		classLoader = new URLClassLoader(classpathUrls.toArray(new URL[classpathUrls.size()]));
	}
}
