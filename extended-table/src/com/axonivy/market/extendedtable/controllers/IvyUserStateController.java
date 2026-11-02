package com.axonivy.market.extendedtable.controllers;

import java.util.List;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.security.IUser;

/**
 * Default controller for the ExtendedTable. It will use the property map of
 * current Ivy user for handling the table state operations.
 * <p>
 * Without a logged-in user (anonymous session) there is no property map: the
 * controller is then not {@link #isAvailable() available}, read operations
 * return empty results and {@link #save(String, String)} is rejected.
 *
 */
public class IvyUserStateController implements TableStateController {
	
	private final IUser currentUser;
	
	public IvyUserStateController() {
		currentUser = Ivy.session().getSessionUser();
	}

	@Override
	public boolean isAvailable() {
		return currentUser != null;
	}

	@Override
	public void save(String key, String stateAsJSON) {
		if (!isAvailable()) {
			throw new IllegalStateException("Cannot save table state '" + key + "': no Ivy user is logged in.");
		}
		currentUser.setProperty(key, stateAsJSON);
	}

	@Override
	public String load(String key) {
		return isAvailable() ? currentUser.getProperty(key) : null;
	}

	@Override
	public boolean delete(String key) {
		return isAvailable() && currentUser.removeProperty(key) != null;
	}

	@Override
	public List<String> listKeys(String prefix) {
		if (!isAvailable()) {
			return List.of();
		}
		return currentUser.getAllPropertyNames().stream().filter(name -> name.startsWith(prefix)).toList();
	}

}
