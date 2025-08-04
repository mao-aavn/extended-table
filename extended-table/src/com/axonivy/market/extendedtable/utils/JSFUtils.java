package com.axonivy.market.extendedtable.utils;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

import org.primefaces.PrimeFaces;
import org.primefaces.util.ComponentTraversalUtils;

import ch.ivyteam.ivy.environment.Ivy;

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

	public static String getRequestParameterValue(String key) {
		return currentContext().getExternalContext().getRequestParameterMap().get(key);
	}

	/**
	 * Update a list of expressions or clientIds.
	 *
	 * @param expressions
	 */
	public static void updateComponents(String... expressions) {
		PrimeFaces.current().ajax().update(expressions);
	}

	/**
	 * @param localId: short id, not clientId (full path)
	 * @return
	 */
	public static UIComponent findComponent(String localId) {
		FacesContext context = FacesContext.getCurrentInstance();
		UIComponent root = context.getViewRoot();

		return ComponentTraversalUtils.firstWithId(localId, root);
	}

	/**
	 * @param localId: short id (e.g buttonSave), not client id (full id: e.g
	 *                 requestForm:buttonSave)
	 */
	public static void updateComponent(String localId) {
		UIComponent component = findComponent(localId);

		if (component != null) {
			FacesContext context = FacesContext.getCurrentInstance();
			String clientId = component.getClientId(context);
			context.getPartialViewContext().getRenderIds().add(clientId);
		} else {
			Ivy.log().warn("Component with ID " + localId + " not found.");
		}
	}

}
