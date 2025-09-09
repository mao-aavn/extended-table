package com.axonivy.market.extendedtable.repo;

import java.util.List;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.security.IUser;

public class IvySessionExtendedDataTableController implements ExtendedDataTableController {

	private IUser getSessionUser() {
		return Ivy.session().getSessionUser();
	}

	@Override
	public void save(String key, String stateAsJSON) {
		IUser currentUser = getSessionUser();
		currentUser.setProperty(key, stateAsJSON);
	}

	@Override
	public String load(String key) {
		IUser currentUser = getSessionUser();
		String stateJson = currentUser.getProperty(key);

		return stateJson;
	}

	@Override
	public boolean delete(String key) {
		IUser currentUser = getSessionUser();

		return currentUser.removeProperty(key) != null;
	}

	@Override
	public List<String> listKeys(String prefix) {
		return getSessionUser()
				.getAllPropertyNames()
				.stream()
				.filter(name -> name.startsWith(prefix))
				.toList();
	}

}
