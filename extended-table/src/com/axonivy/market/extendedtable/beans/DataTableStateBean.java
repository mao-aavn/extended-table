package com.axonivy.market.extendedtable.beans;

import static com.axonivy.market.extendedtable.utils.JSFUtils.findComponent;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

import com.axonivy.market.extendedtable.utils.Attrs;
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
public class DataTableStateBean implements Serializable {

	private static final String TABLE_ID = "tableId";
	private static final long serialVersionUID = -5460403522329131748L;
	private static final String STATE_KEY_PREFIX = "DATATABLE_";
	private static final String STATE_KEY_PATTERN = STATE_KEY_PREFIX + "%s_%s";
	private String stateName;
	private List<String> stateNames = new ArrayList<>();

	private String getTableClientId() {
		UIComponent tableComponent = findComponent((String) Attrs.currentContext().get(TABLE_ID));

		if (tableComponent == null) {
			throw new IllegalStateException(
					"Component with id '" + Attrs.currentContext().get(TABLE_ID) + "' not found in view.");
		}

		return tableComponent.getClientId();
	}

	public void saveTableState() {
		DataTable table = (DataTable) getViewRoot().findComponent(getTableClientId());

		if (table != null) {
			DataTableState state = table.getMultiViewState(false);
			if (state != null) {
				saveTableStateToIvyUser(state);
			} else {
				Ivy.log().warn("State is null for the table: %s", getTableClientId());
			}
		}

		stateNames = getAllCurrentTableStateNames();
	}

	public void restoreTableState() {
		UIViewRoot viewRoot = getViewRoot();

		DataTable currentTable = (DataTable) viewRoot.findComponent(getTableClientId());

		if (currentTable == null) {
			Ivy.log().warn("Table not found with the given id: {0}", getTableClientId());
			return;
		}

		if (currentTable.getFilteredValue() == null) {
			currentTable.setFilteredValue(new ArrayList<>());
		}

		currentTable.reset();

		DataTableState persistedState = getTableStateFromIvyUser();

		if (persistedState != null) {
			DataTableState currentState = currentTable.getMultiViewState(true); // force create
			currentState.setFilterBy(persistedState.getFilterBy());
			currentState.setSortBy(persistedState.getSortBy());
			currentState.setFirst(persistedState.getFirst());
			currentState.setColumnMeta(persistedState.getColumnMeta());
			currentState.setExpandedRowKeys(persistedState.getExpandedRowKeys());
			currentState.setRows(persistedState.getRows());
			currentState.setSelectedRowKeys(persistedState.getSelectedRowKeys());
			currentState.setWidth(persistedState.getWidth());
			currentTable.setFirst(persistedState.getFirst());

			currentTable.filterAndSort();
			currentTable.resetColumns();
		} else {
			Ivy.log().warn("No saved table state to restore for the table %s and state %s", getTableClientId(),
					stateName);
		}
	}

	public void resetTable() {
		String viewId = getViewRoot().getViewId();
		PrimeFaces.current().multiViewState().clearAll(viewId, true, null);
		stateName = null;
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

	public DataTableState getTableStateFromIvyUser() {
		String stateKey = getStateKey();
		String stateJson = getCachingData(stateKey);

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule()).addMixIn(FilterMeta.class, FilterDataTableMixin.class)
				.addMixIn(SortMeta.class, SortDataTableMixin.class);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		DataTableState tableState = null;
		try {
			tableState = mapper.readValue(stateJson, new TypeReference<DataTableState>() {
			});
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
		if (stateNames.isEmpty()) {
			stateNames = getAllCurrentTableStateNames();
		}

		return stateNames.stream().filter(name -> name != null && name.toLowerCase().contains(query.toLowerCase()))
				.toList();
	}

	public void deleteTableState() {
		String stateKey = getStateKey();
		IUser currentUser = getSessionUser();
		currentUser.removeProperty(stateKey);

		// Update stateNames list after deletion
		stateNames = getAllCurrentTableStateNames();
		if (stateNames.size() > 0) {
			stateName = stateNames.get(0);
		}
	}

	private String getCachingData(String stateKey) {
		IUser currentUser = getSessionUser();
		String stateJson = currentUser.getProperty(stateKey);

		return stateJson;
	}

	protected void setCachingData(String stateKey, String stateJson) {
		IUser currentUser = getSessionUser();
		currentUser.setProperty(stateKey, stateJson);
	}

	protected List<String> getCacheKeys() {
		return getSessionUser().getAllPropertyNames();
	}

	private UIViewRoot getViewRoot() {
		return FacesContext.getCurrentInstance().getViewRoot();
	}

	private IUser getSessionUser() {
		return Ivy.session().getSessionUser();
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
