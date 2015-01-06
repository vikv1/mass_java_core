package edu.uw.bothell.css.dsl.MASS.factory;

import java.io.File;
import java.lang.reflect.Constructor;
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
		
		try {
			newClass = (Class<T>) Class.forName( className, true, classLoader ); 
		}
		catch (ClassNotFoundException e) {
			throw new Exception("Unable to find " + className, e);
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
	
		T newObjectInstance =  newClassConstructor.newInstance( constructorArgument );
		
		return newObjectInstance;
	
	}
	
	/**
	 * Set the classloader used by this factory, using the URLs previously set
	 */
	private void initClassLoader() {
		classLoader = new URLClassLoader(classpathUrls.toArray(new URL[classpathUrls.size()]));
	}

}
