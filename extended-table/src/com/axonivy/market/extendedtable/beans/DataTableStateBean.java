package com.axonivy.market.extendedtable.beans;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

import org.primefaces.PrimeFaces;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.datatable.DataTableState;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;
import org.primefaces.model.filter.FilterConstraint;
import org.primefaces.util.ComponentTraversalUtils;

import com.axonivy.market.extendedtable.utils.Attrs;
import com.axonivy.market.extendedtable.utils.JSFUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.security.IUser;

@ViewScoped
@ManagedBean(name = "dataTableStateBean")
public class DataTableStateBean<T> implements Serializable {

	private static final long serialVersionUID = -5460403522329131748L;
	private static final String STATE_KEY_PREFIX = "DATATABLE_";
	private static final String STATE_KEY_PATTERN = STATE_KEY_PREFIX + "%s_%s";
	private String stateName;
	private List<String> stateNames;

	protected String getTableClientId() {
		UIComponent tableComponent = findComponent((String) Attrs.currentContext().get("tableId"));

		if (tableComponent == null) {
			throw new IllegalStateException(
					"Component with id '" + Attrs.currentContext().get("tableId") + "' not found in view.");
		}

		return tableComponent.getClientId();
	}

	public static UIComponent findComponent(String localId) {
		FacesContext context = FacesContext.getCurrentInstance();
		UIComponent root = context.getViewRoot();

		return ComponentTraversalUtils.firstWithId(localId, root);
	}

	public void saveTableState() {
		Ivy.log().info("saveTableState with stateName --- " + stateName);
		Ivy.log().info("getTableClientId() --- " + getTableClientId());
		DataTable table = (DataTable) FacesContext.getCurrentInstance().getViewRoot().findComponent(getTableClientId());

		if (table != null) {
			Ivy.log().info("EVENTS " + table.getEventNames());

			DataTableState state = table.getMultiViewState(false);
			if (state != null) {
				saveTableStateToIvyUser(state);
				Ivy.log().info("Saved table state.");
			} else {
				Ivy.log().warn("State is null for the table: %s", getTableClientId());
			}
		} else {
			Ivy.log().info("Table with the given id not found: %s", getTableClientId());
		}

		stateNames = getAllCurrentTableStateNames();
		PrimeFaces.current().ajax().update("stateNameInput");
	}

	public void restoreTableState() {
		Ivy.log().warn("stateName: " + stateName);
		// clearMultiViewState();

		FacesContext context = FacesContext.getCurrentInstance();
		UIViewRoot viewRoot = context.getViewRoot();

		DataTable currentTable = (DataTable) viewRoot.findComponent(getTableClientId());

		if (currentTable == null) {
			Ivy.log().warn("Table not found with the given id: {0}", getTableClientId());
			return;
		}

		// Ensure filteredValue is never null
		if (currentTable.getFilteredValue() == null) {
			currentTable.setFilteredValue(new ArrayList<>());
		}

		currentTable.reset();

		DataTableState saved = getTableStateFromIvyUser();

		if (saved != null) {

			DataTableState currentState = currentTable.getMultiViewState(true); // force create
			Ivy.log().info("saved.getFilterBy() " + saved.getFilterBy());
			Map<String, FilterMeta> filterBy = saved.getFilterBy();

			if (filterBy != null) {
				for (Map.Entry<String, FilterMeta> entry : filterBy.entrySet()) {
					FilterMeta meta = entry.getValue();
					String field = meta.getField();
					Object filterValue = meta.getFilterValue();
					if (filterValue != null) {
						Ivy.log().warn("Filter Field: " + field + " | Value: " + filterValue);
					}
				}
			}

			// Log sorting (order) only if not null and not UNSORTED
			Map<String, SortMeta> sortBy = saved.getSortBy();
			if (sortBy != null) {
				for (Map.Entry<String, SortMeta> entry : sortBy.entrySet()) {
					SortMeta meta = entry.getValue();
					String field = meta.getField();
					Object order = meta.getOrder();
					if (order != null && !"UNSORTED".equals(order.toString())) {
						Ivy.log().warn("Sort Field: " + field + " | Order: " + order);
					}
				}
			}

			// Log page (first row index) only if not null and > 0
			int first = saved.getFirst();

			Ivy.log().warn("Page First Row Index: " + first);

			currentState.setFilterBy(saved.getFilterBy());
			currentState.setSortBy(saved.getSortBy());
			currentState.setFirst(saved.getFirst());
			currentState.setColumnMeta(saved.getColumnMeta());
			currentState.setExpandedRowKeys(saved.getExpandedRowKeys());
			currentState.setRows(saved.getRows());
			currentState.setSelectedRowKeys(saved.getSelectedRowKeys());
			currentState.setWidth(saved.getWidth());

			// Jump to the restored page
			currentTable.setFirst(saved.getFirst());

			// Explicitly trigger filter and sort after restoring state
			currentTable.filterAndSort();
			currentTable.resetColumns();
			PrimeFaces.current().ajax().update(currentTable);

		} else {
			Ivy.log().warn("No saved table state to restore for the table %s and state %s", getTableClientId(),
					stateName);
		}
	}

	public void resetTable() {
		FacesContext context = FacesContext.getCurrentInstance();
		String viewId = context.getViewRoot().getViewId();
		PrimeFaces.current().multiViewState().clearAll(viewId, true, null);
		stateName = null;
		JSFUtils.updateComponents(getTableClientId());
	}

	private void saveTableStateToIvyUser(DataTableState state) {
		String stateKey = getStateKey();

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule()).addMixIn(FilterMeta.class, FilterDataTableMixin.class)
				.addMixIn(SortMeta.class, SortDataTableMixin.class);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		try {
			String stateJson = mapper.writeValueAsString(state);
			setCachingData(stateKey, stateJson);

		} catch (JsonProcessingException e) {
			Ivy.log().error("Couldn't serialize TableState to JSON", e);
		}

		getTableStateFromIvyUser();
	}

	protected String getCachingData(String stateKey) {
		IUser currentUser = Ivy.session().getSessionUser();
		String stateJson = currentUser.getProperty(stateKey);

		return stateJson;
	}

	protected void setCachingData(String stateKey, String stateJson) {
		IUser currentUser = Ivy.session().getSessionUser();
		currentUser.setProperty(stateKey, stateJson);
		Ivy.log().info("===== SAVED STATE ====" + stateKey);
		Ivy.log().info(stateKey + "=>" + currentUser.getProperty(stateKey));
	}
	protected List<String> getCacheKeys() {
		return Ivy.session().getSessionUser().getAllPropertyNames();
	}

	public DataTableState getTableStateFromIvyUser() {
		String stateKey = getStateKey();
		String stateJson = getCachingData(stateKey);

		if (stateJson == null || stateJson.isEmpty()) {
			Ivy.log().info("No table state found for key: " + stateKey);
		} else {
			Ivy.log().info("===== FETCHED STATE ====" + stateKey);
			Ivy.log().info(stateKey + "=>" + stateJson);
		}

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule()).addMixIn(FilterMeta.class, FilterDataTableMixin.class)
				.addMixIn(SortMeta.class, SortDataTableMixin.class);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		DataTableState tableState = null;
		try {
			tableState = mapper.readValue(stateJson, new TypeReference<DataTableState>() {
			});
			Ivy.log().info("DataTableState: " + tableState);
		} catch (IOException e) {
			Ivy.log().error("Couldn't deserialize TableState from JSON", e);
		}

		return tableState;
	}

	public List<String> getAllCurrentTableStateNames() {
		return getCacheKeys().stream().filter(name -> name.startsWith(STATE_KEY_PREFIX + getTableClientId()))
				.map(name -> {
					// Remove prefix
					String remainder = name.substring(STATE_KEY_PREFIX.length());
					// Get the part after the first underscore
					int firstUnderscoreIndex = remainder.indexOf('_');
					return firstUnderscoreIndex > 0 ? remainder.substring(firstUnderscoreIndex + 1) : "";
				}).filter(value -> !value.isEmpty()).toList();
	}

	public List<String> completeStateName(String query) {
		if (stateNames == null) {
			stateNames = getAllCurrentTableStateNames();
		}

		return stateNames.stream().filter(name -> name != null && name.toLowerCase().contains(query.toLowerCase()))
				.toList();
	}

	public void deleteTableState() {
		String stateKey = getStateKey();
		IUser currentUser = Ivy.session().getSessionUser();
		currentUser.removeProperty(stateKey);

		// Update stateNames list after deletion
		stateNames = getAllCurrentTableStateNames();
		if (stateNames.size() > 0) {
			stateName = stateNames.get(0);
		} else {
			stateName = null;
		}
		PrimeFaces.current().ajax().update("@form");
	}

	private String getStateKey() {
		return String.format(STATE_KEY_PATTERN, getTableClientId(), stateName);
	}

	public String getStateName() {
		return stateName;
	}

	public void setStateName(String stateName) {
		this.stateName = stateName;
	}

	public List<String> getStateNames() {
		return stateNames;
	}

	public void setStateNames(List<String> stateNames) {
		this.stateNames = stateNames;
	}

	public abstract static class SortDataTableMixin {
		@JsonIgnore
		public abstract javax.el.ValueExpression getSortBy();

		@JsonIgnore
		public abstract javax.el.ValueExpression getSortFunction();

		@JsonIgnore
		public abstract Boolean getIsActive();
	}

	public abstract class FilterDataTableMixin {

		@JsonIgnore
		ValueExpression filterBy;

		@JsonIgnore
		FilterConstraint constraint;

		@JsonIgnore
		abstract boolean isActive();
	}

}
