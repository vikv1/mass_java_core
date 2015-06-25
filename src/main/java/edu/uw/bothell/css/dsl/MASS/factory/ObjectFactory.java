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
