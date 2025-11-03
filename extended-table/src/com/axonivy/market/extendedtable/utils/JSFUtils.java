package com.axonivy.market.extendedtable.utils;

import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.ExternalContext;

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
	 * Convenience helper to read a request parameter from the current FacesContext.
	 * Returns null if context or external context or parameter map is not available.
	 */
	public static String getRequestParam(String name) {
		FacesContext ctx = currentContext();
		if (ctx == null) {
			return null;
		}
		ExternalContext ext = ctx.getExternalContext();
		if (ext == null) {
			return null;
		}
		Map<String, String> params = ext.getRequestParameterMap();
		return params == null ? null : params.get(name);
	}

	/**
	 * @param localId: short id, not clientId (full path)
	 */
	public static UIComponent findComponent(String localId) {
		UIComponent root = currentContext().getViewRoot();

		return ComponentTraversalUtils.firstWithId(localId, root);
	}
}
