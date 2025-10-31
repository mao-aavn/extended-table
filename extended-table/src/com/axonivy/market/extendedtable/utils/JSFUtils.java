package com.axonivy.market.extendedtable.utils;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

import org.primefaces.util.ComponentTraversalUtils;

public final class JSFUtils {

	private JSFUtils() {
	}

	public static void addErrorMsg(String clientId, String summaryMsg, String detailMsg) {
		addMessage(clientId, FacesMessage.SEVERITY_ERROR, summaryMsg, detailMsg);
	}

	public static void addInfoMsg(String clientId, String summaryMsg, String detailMsg) {
		addMessage(clientId, FacesMessage.SEVERITY_INFO, summaryMsg, detailMsg);
	}

	private static void addMessage(String clientId, Severity severity, String summaryMsg, String detailMsg) {
		currentContext().addMessage(clientId, new FacesMessage(severity, summaryMsg, detailMsg));
	}

	public static FacesContext currentContext() {
		return FacesContext.getCurrentInstance();
	}

	public static UIViewRoot getViewRoot() {
		return currentContext().getViewRoot();
	}

	public static UIComponent findComponentFromClientId(String clientId) {
		return getViewRoot().findComponent(clientId);
	}

	/**
	 * @param localId: short id, not clientId (full path)
	 */
	public static UIComponent findComponent(String localId) {
		UIComponent root = currentContext().getViewRoot();

		return ComponentTraversalUtils.firstWithId(localId, root);
	}

}
