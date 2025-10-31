package com.axonivy.market.extendedtable.controllers;

import java.util.List;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.security.IUser;

/**
 * Default controller for the ExtendedTable. It will use the property map of
 * current Ivy user for handling the table state operations.
 *
 */
public class IvyUserStateController implements TableStateController {
	
	private final IUser currentUser;
	
	public IvyUserStateController() {
		currentUser = Ivy.session().getSessionUser();
	}

	@Override
	public void save(String key, String stateAsJSON) {
		currentUser.setProperty(key, stateAsJSON);
	}

	@Override
	public String load(String key) {
		String stateJson = currentUser.getProperty(key);

		return stateJson;
	}

	@Override
	public boolean delete(String key) {
		return currentUser.removeProperty(key) != null;
	}

	@Override
	public List<String> listKeys(String prefix) {
		return currentUser.getAllPropertyNames().stream().filter(name -> name.startsWith(prefix)).toList();
	}

}
