package com.axonivy.market.extendedtable.demo.utils;

import java.util.HashMap;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.primefaces.PrimeFaces;
import org.primefaces.component.fieldset.Fieldset;
import org.primefaces.component.outputlabel.OutputLabel;
import org.primefaces.model.FilterMeta;
import org.primefaces.util.ComponentTraversalUtils;

import ch.ivyteam.ivy.environment.Ivy;

public final class JSFUtils {
	public static final String JSF_SOURCE_PARAM = "javax.faces.source";
	private static final int TREE_DEPTH_SEACH_LEVEL = 10;

	private JSFUtils() {
	}

	private static FacesContext currentContext() {
		return FacesContext.getCurrentInstance();
	}

	public static Object findSubmittingValue(String clientId) {
		UIInput component = (UIInput) currentContext().getViewRoot().findComponent(clientId);

		return component.getSubmittedValue();
	}

	public static void showDialog(String dialogName) {
		PrimeFaces.current().executeScript("PF('" + dialogName + "').show();");
	}

	public static void hideDialog(String dialogName) {
		PrimeFaces.current().executeScript("PF('" + dialogName + "').hide();");
	}

	/**
	 * Update a list of expressions or clientIds.
	 * 
	 * @param expressions
	 */
	public static void updateComponents(String... expressions) {
		PrimeFaces.current().ajax().update(expressions);
	}

	public static String getCurrentComponentId() {
		return UIComponent.getCurrentComponent(FacesContext.getCurrentInstance()).getId();
	}

	public static Fieldset findClosestParentFieldset() {
		UIComponent currentComponent = UIComponent.getCurrentComponent(FacesContext.getCurrentInstance());
		if (currentComponent instanceof Fieldset) {
			return null;
		}

		int counter = 0;

		while (currentComponent != null && counter++ < TREE_DEPTH_SEACH_LEVEL
				&& !(currentComponent instanceof Fieldset)) {
			currentComponent = currentComponent.getParent();
		}

		return currentComponent instanceof Fieldset ? (Fieldset) currentComponent : null;
	}

	public static boolean isButtonClicked(FacesContext context, String buttonId) {
		String[] sourceIds = context.getExternalContext().getRequestParameterValuesMap().get(JSF_SOURCE_PARAM);
		if (sourceIds == null || sourceIds.length == 0) {
			return false;
		}

		return sourceIds[0].contentEquals(buttonId);
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

	public static void updateComponentWithClientId(String clientId) {
		PrimeFaces.current().ajax().update(clientId);
	}

	public static void updateComponentWithWidgetVar(String widgetVar) {
		PrimeFaces.current().executeScript("PF('" + widgetVar + "').update()");
	}

	public static void highLightErrorComponent(String id) {
		final UIComponent c = findComponent(id);
		highLightErrorComponent(c);
	}

	public static void highLightErrorComponent(UIComponent c) {
		final String scriptPattern = "document.getElementById('%s').classList.add('%s');";
		PrimeFaces.current().executeScript(String.format(scriptPattern, c.getClientId(), "error-component"));
	}

	public static UIComponent findOutputLabel(UIComponent root, String forValue) {
		if (root == null || forValue == null) {
			return null;
		}

		if (root instanceof OutputLabel && forValue.equals(root.getAttributes().get("for"))) {
			return root;
		}

		// Recursively search through the children
		for (UIComponent child : root.getChildren()) {
			UIComponent found = findOutputLabel(child, forValue);
			if (found != null) {
				return found;
			}
		}

		return null; // Not found
	}

	public static Map<String, Object> buildDatatableFilterMap(Map<String, FilterMeta> filterBy) {
		Map<String, Object> filters = new HashMap<>();
		if (filterBy != null) {
			for (Map.Entry<String, FilterMeta> entry : filterBy.entrySet()) {
				filters.put(entry.getKey(), entry.getValue().getFilterValue());
			}
		}
		return filters;
	}

}
