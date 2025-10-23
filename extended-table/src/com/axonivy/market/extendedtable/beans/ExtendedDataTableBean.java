package com.axonivy.market.extendedtable.beans;

import static com.axonivy.market.extendedtable.utils.JSFUtils.addErrorMsg;
import static com.axonivy.market.extendedtable.utils.JSFUtils.addInfoMsg;
import static com.axonivy.market.extendedtable.utils.JSFUtils.findComponent;
import static com.axonivy.market.extendedtable.utils.JSFUtils.findComponentFromClientId;
import static com.axonivy.market.extendedtable.utils.JSFUtils.getViewRoot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.el.ValueExpression;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.datatable.DataTableState;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;
import org.primefaces.model.filter.FilterConstraint;

import com.axonivy.market.extendedtable.controllers.ExtendedDataTableController;
import com.axonivy.market.extendedtable.controllers.IvySessionExtendedDataTableController;
import com.axonivy.market.extendedtable.utils.Attrs;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ch.ivyteam.ivy.environment.Ivy;

@ViewScoped
@ManagedBean(name = "extendedDataTableBean")
public class ExtendedDataTableBean {

	private static final String TABLE_ID = "tableId";
	private static final String DATA_TABLE_STATE_REPOSITORY = "dataTableStateRepository";
	private static final String GROWL_MSG_ID = "extendedTableGrowlMsg";
	private static final String STATE_KEY_PREFIX = "DATATABLE_";
	private static final String STATE_KEY_PATTERN = STATE_KEY_PREFIX + "%s_%s";
	private String stateName;
	private List<String> stateNames = new ArrayList<>();
	private String saveButtonClicked = null;

	public void saveTableState() {
		// String saveButtonClick = getRequestParameterValue("saveButtonClick");
		// Ignore save if not called from the explicit save button
		Ivy.log().info("saveButtonClicked");
		Ivy.log().info(saveButtonClicked);
		if (StringUtils.isEmpty(saveButtonClicked)) {
			return;
		}

		if (stateName == null || stateName.isEmpty()) {
			addErrorMsg(GROWL_MSG_ID, "State name is required!", null);
		} else {
			String tableClientId = getTableClientId();
			DataTable table = (DataTable) findComponentFromClientId(tableClientId);

			if (table != null) {
				DataTableState state = table.getMultiViewState(false);
				if (state != null) {
					persistDataTableState(state);
				} else {
					Ivy.log().warn("State is null for the table: {0}", getTableClientId());
				}
			}

			stateNames = fetchAllDataTableStateNames();
			addInfoMsg(GROWL_MSG_ID, "Saved the table state successfully", null);
		}
		saveButtonClicked = null;
	}

	public void restoreTableState() {
		DataTable currentTable = (DataTable) findComponentFromClientId(getTableClientId());

		if (currentTable == null) {
			Ivy.log().warn("Table not found with the given id: {0}", getTableClientId());
			return;
		}

		if (currentTable.getFilteredValue() == null) {
			currentTable.setFilteredValue(new ArrayList<>());
		}

		currentTable.reset();

		DataTableState persistedState = fetchDataTableState();

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
		String tableClientId = getTableClientId();
		PrimeFaces.current().multiViewState().clearAll(viewId, true, null);
		stateName = null;
		DataTable currentTable = (DataTable) findComponentFromClientId(tableClientId);

		if (currentTable == null) {
			Ivy.log().warn("Table not found with the given id: {0}", tableClientId);
			return;
		}

		currentTable.clearInitialState();
		currentTable.resetColumns();
	}

	public void deleteTableState() {
		String stateKey = getStateKey();
		if (!getController().delete(stateKey)) {
			addErrorMsg(stateKey, "No existing name " + stateKey, stateKey);
		} else {
			stateNames = fetchAllDataTableStateNames();
			if (stateNames.size() > 0) {
				stateName = stateNames.get(0);
			} else {
				stateName = null;
			}
			addInfoMsg(GROWL_MSG_ID, "Delete the state successfully", null);
		}
	}

	public List<String> completeStateName(String query) {
		if (stateNames == null || stateNames.isEmpty()) {
			stateNames = fetchAllDataTableStateNames();
			Ivy.log().info("stateNames : " + stateNames);
		}
		if (stateNames == null || stateNames.isEmpty()) {
			Ivy.log().info("stateNames are tempty: ");
			return Collections.emptyList();
		}

		if (query == null || query.trim().isEmpty()) {
			return stateNames;
		}

		return stateNames.stream().filter(name -> name != null && name.toLowerCase().contains(query.toLowerCase()))
				.toList();
	}

	private void persistDataTableState(DataTableState state) {
		String stateKey = getStateKey();

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule()).addMixIn(FilterMeta.class, FilterDataTableMixin.class)
				.addMixIn(SortMeta.class, SortDataTableMixin.class);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		try {
			String stateJson = mapper.writeValueAsString(state);
			getController().save(stateKey, stateJson);
		} catch (JsonProcessingException e) {
			Ivy.log().error("Couldn't serialize TableState to JSON", e);
		}
	}

	private DataTableState fetchDataTableState() {
		String stateKey = getStateKey();
		String stateJson = getController().load(stateKey);

		Ivy.log().info(stateKey);
		Ivy.log().info(stateJson);

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

	private List<String> fetchAllDataTableStateNames() {
		String tableId = getTableClientId();
		String prefix = STATE_KEY_PREFIX + tableId + "_";

		return getController().listKeys(prefix).stream().filter(name -> name.startsWith(prefix))
				.map(name -> name.substring(prefix.length())).filter(value -> !value.isEmpty()).toList();
	}

	private String getStateKey() {
		return String.format(STATE_KEY_PATTERN, getTableClientId(), stateName);
	}

	/**
	 * In case dataTableStateRepository attribute is passed to the ExtendedTable
	 * component, it will be used for persisting the table state, otherwise it will
	 * fallback to the default SessionDataTableStateRepository which persists the state data to the Ivy User's
	 * property map.
	 * 
	 */
	private ExtendedDataTableController getController() {
		ExtendedDataTableController repo = (ExtendedDataTableController) Attrs.currentContext()
				.get(DATA_TABLE_STATE_REPOSITORY);
		if (repo != null && repo instanceof ExtendedDataTableController) {
			return (ExtendedDataTableController) repo;
		}

		// Fallback to default in case no overriding controller is set
		return new IvySessionExtendedDataTableController();
	}

	private String getTableClientId() {
		UIComponent tableComponent = findComponent((String) Attrs.currentContext().get(TABLE_ID));

		if (tableComponent == null) {
			throw new IllegalStateException(
					"Component with id '" + Attrs.currentContext().get(TABLE_ID) + "' not found in view.");
		}

		return tableComponent.getClientId();
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

	public String getSaveButtonClicked() {
		return saveButtonClicked;
	}

	public void setSaveButtonClicked(String saveButtonClicked) {
		this.saveButtonClicked = saveButtonClicked;
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
