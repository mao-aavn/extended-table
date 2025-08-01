package com.axonivy.market.extendedtable.utils;

import java.util.List;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.security.IUser;

public class IvySessionCacheUtils {

	public static IUser getSessionUser() {
		return Ivy.session().getSessionUser();
	}

	public static String removeProperty(String key) {
		IUser currentUser = getSessionUser();

		return currentUser.removeProperty(key);
	}

	public static String getPropertyDataFromSession(String key) {
		IUser currentUser = getSessionUser();
		String stateJson = currentUser.getProperty(key);

		return stateJson;
	}

	public static void setPropertyDataToSession(String key, String value) {
		IUser currentUser = getSessionUser();
		currentUser.setProperty(key, value);
	}

	public static List<String> getAllSessionPropertieNames() {
		return getSessionUser().getAllPropertyNames();
	}

}
