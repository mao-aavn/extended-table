package com.axonivy.market.extendedtable.controllers;

import java.util.List;

/**
 * Controller to handle the data table state operations and so on.
 *
 */
public interface ExtendedDataTableController {

	/**
	 * Save the state JSON string with key
	 * 
	 * @param key
	 * @param stateAsJSON
	 */
	void save(String key, String stateAsJSON);

	/**
	 * Load the state as JSON string with the given key
	 * 
	 * @param key
	 * @return State JSON string
	 */
	String load(String key);

	/**
	 * Delete the state with the given key
	 * 
	 * @param key
	 * @return true if delete successfully, otherwise false
	 */
	boolean delete(String key);

	/**
	 * List all keys with the given prefix.
	 * 
	 * @param prefix
	 * @return
	 */
	List<String> listKeys(String prefix);
}