package com.axonivy.market.extendedtable.utils;

import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.ExternalContext;

import org.primefaces.util.ComponentTraversalUtils;
import org.primefaces.PrimeFaces;

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

	/**
	 * Execute arbitrary JavaScript on the client via PrimeFaces.
	 */
	public static void executeScript(String script) {
		PrimeFaces.current().executeScript(script);
	}

	/**
	 * Copy the provided text to the client's clipboard by executing a small
	 * navigator.clipboard.writeText(...) script. The text will be escaped to be
	 * safely embedded into a single-quoted JavaScript string literal.
	 */
	public static void copyToClipboard(String text) {
		if (text == null) {
			return;
		}
		// Escape backslashes and single quotes and normalize newlines for JS string
		String jsSafe = text.replace("\\", "\\\\").replace("'", "\\'")
				.replace("\n", "\\n").replace("\r", "");
		String script = "navigator.clipboard.writeText('" + jsSafe
				+ "').then(function(){console.log('copied to clipboard');}).catch(function(e){console.error(e);});";
		executeScript(script);
	}
}
