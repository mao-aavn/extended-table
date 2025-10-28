package com.axonivy.market.extendedtable.beans;

import static com.axonivy.market.extendedtable.utils.JSFUtils.addErrorMsg;
import static com.axonivy.market.extendedtable.utils.JSFUtils.addInfoMsg;
import static com.axonivy.market.extendedtable.utils.JSFUtils.findComponent;
import static com.axonivy.market.extendedtable.utils.JSFUtils.findComponentFromClientId;
import static com.axonivy.market.extendedtable.utils.JSFUtils.getViewRoot;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

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

/**
 * Bean for ExtendedTable HTML component.
 *
 */
public class ExtendedDataTableBean {

	private static final String TABLE_ID = "tableId";
	private static final String DATA_TABLE_STATE_REPOSITORY = "dataTableStateRepository";
	private static final String GROWL_MSG_ID = "extendedTableGrowlMsg";
	private static final String STATE_KEY_PREFIX = "DATATABLE_";
	private static final String STATE_KEY_PATTERN = STATE_KEY_PREFIX + "%s_%s";
	private String stateName;
	private List<String> stateNames = new ArrayList<>();

	public void saveTableState() {
		// Only save if explicitly triggered by the Save button via the explicitSave parameter
		// This prevents accidental saves when filtering or other actions trigger form submission
		String explicitSave = FacesContext.getCurrentInstance().getExternalContext()
				.getRequestParameterMap().get("explicitSave");
		
		Ivy.log().info("saveTableState called but explicitSave parameter is not 'true', skipping save. Value: {0}", explicitSave);
		if (!"true".equals(explicitSave)) {
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
					stateNames = fetchAllDataTableStateNames();
					addInfoMsg(GROWL_MSG_ID, "Saved the table state successfully", null);
				} else {
					Ivy.log().warn("State is null for the table: {0}", getTableClientId());
				}
			}
		}
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
			// Convert date strings back to LocalDate objects for proper filtering
			convertDateStringsToLocalDates(persistedState);
			
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

			// Apply filters and sorting first to get the correct filtered dataset
			currentTable.filterAndSort();
			currentTable.resetColumns();
			
			// Restore selection AFTER filterAndSort so we work with filtered items
			restoreSelection(currentTable, persistedState.getSelectedRowKeys());
		} else {
			Ivy.log().warn("No saved table state to restore for the table %s and state %s", getTableClientId(),
					stateName);
		}
	}

	/**
	 * Restores the selection by converting row keys back to actual objects.
	 * PrimeFaces stores row keys in the state, but the selection List needs
	 * to be populated with the actual objects for checkboxes to be checked.
	 * 
	 * @param table The DataTable component
	 * @param selectedRowKeys The set of row keys from the persisted state
	 */
	private void restoreSelection(DataTable table, Set<String> selectedRowKeys) {
		Ivy.log().info("restoreSelection called with selectedRowKeys: {0}", selectedRowKeys);
		
		if (selectedRowKeys == null || selectedRowKeys.isEmpty()) {
			Ivy.log().info("No selected row keys to restore");
			return;
		}
		
		// Get the selection value expression from the composite component attributes
		// The DataTable gets its selection from cc.attrs.selection in the composite component
		Object selectionAttr = Attrs.currentContext().get("selection");
		Ivy.log().info("Selection attribute from composite component: {0}", selectionAttr);
		
		if (selectionAttr == null) {
			Ivy.log().warn("No selection attribute found on composite component");
			return;
		}
		
		// The selection attribute should already be the actual object/list from the bean
		// since it's evaluated via #{cc.attrs.selection} in the composite component
		Ivy.log().info("Selection object type: {0}", selectionAttr.getClass().getName());
		// The selection attribute should already be the actual object/list from the bean
		// since it's evaluated via #{cc.attrs.selection} in the composite component
		Ivy.log().info("Selection object type: {0}", selectionAttr.getClass().getName());
		
		// Only handle List selections (for multiple selection mode)
		if (!(selectionAttr instanceof List)) {
			Ivy.log().warn("Selection is not a List, cannot restore. Type: {0}", selectionAttr.getClass().getName());
			return;
		}
		
		@SuppressWarnings("unchecked")
		List<Object> selectionList = (List<Object>) selectionAttr;
		Ivy.log().info("Current selection list size before clear: {0}", selectionList.size());
		selectionList.clear();
		
		FacesContext context = FacesContext.getCurrentInstance();
		
		// Get the value (data source) from the table - use filtered value if available
		Object value = table.getFilteredValue();
		if (value == null || !(value instanceof List)) {
			// Fallback to full value if no filtered value
			value = table.getValue();
		}
		
		Ivy.log().info("Using filtered value: {0}", table.getFilteredValue() != null);
		
		if (!(value instanceof List)) {
			Ivy.log().warn("Table value is not a List. Type: {0}", 
				value != null ? value.getClass().getName() : "null");
			return;
		}
		
		@SuppressWarnings("unchecked")
		List<?> items = (List<?>) value;
		Ivy.log().info("Table has {0} items to check", items.size());
		
		// Get the rowKey value expression to extract keys from items
		ValueExpression rowKeyVE = table.getValueExpression("rowKey");
		if (rowKeyVE == null) {
			Ivy.log().warn("No rowKey value expression found on table");
			return;
		}
		Ivy.log().info("RowKey value expression: {0}", rowKeyVE.getExpressionString());
		
		String var = table.getVar();
		Ivy.log().info("Table var name: {0}", var);
		
		// Match items by their row keys and add to selection
		int matchedCount = 0;
		for (Object item : items) {
			context.getExternalContext().getRequestMap().put(var, item);
			Object rowKey = rowKeyVE.getValue(context.getELContext());
			
			if (rowKey != null && selectedRowKeys.contains(rowKey.toString())) {
				selectionList.add(item);
				matchedCount++;
				Ivy.log().info("Matched item with rowKey: {0}", rowKey);
			}
		}
		
		Ivy.log().info("Restored {0} selected items out of {1} row keys", matchedCount, selectedRowKeys.size());
		Ivy.log().info("Final selection list size: {0}", selectionList.size());
		
		// Set the selection on the DataTable itself
		table.setSelection(selectionList);
		Ivy.log().info("Set selection on DataTable");
		
		// For checkbox selection, we need to explicitly update the selected row keys
		// so PrimeFaces knows which checkboxes to check across pages
		// Note: We need to get the state and update it there, as getSelectedRowKeys() may return immutable Set
		DataTableState state = table.getMultiViewState(false);
		if (state != null) {
			Set<String> stateSelectedRowKeys = state.getSelectedRowKeys();
			Ivy.log().info("State.getSelectedRowKeys() returned: {0}", stateSelectedRowKeys);
			
			if (stateSelectedRowKeys != null) {
				Ivy.log().info("Current state selectedRowKeys before clear: {0}", stateSelectedRowKeys);
				stateSelectedRowKeys.clear();
				stateSelectedRowKeys.addAll(selectedRowKeys);
				Ivy.log().info("Updated state selectedRowKeys, size: {0}, keys: {1}", 
					stateSelectedRowKeys.size(), stateSelectedRowKeys);
			} else {
				Ivy.log().warn("State selectedRowKeys is null, creating new HashSet");
				// If null, create a new HashSet and set it
				Set<String> newKeys = new java.util.HashSet<>(selectedRowKeys);
				state.setSelectedRowKeys(newKeys);
				Ivy.log().info("Created new selectedRowKeys set with size: {0}", newKeys.size());
			}
		} else {
			Ivy.log().warn("DataTableState is null, cannot sync checkbox state");
		}
		
		// Force client-side checkbox update by triggering a row select event
		// This is necessary because modifying selectedRowKeys on the server doesn't automatically update the UI
		StringBuilder script = new StringBuilder();
		script.append("setTimeout(function() {");
		script.append("  var widget = PF('").append(getWidgetVar()).append("');");
		script.append("  if (widget && widget.selection) {");
		
		// Build array of selected row keys for client-side
		script.append("    widget.selection = [");
		boolean first = true;
		for (String key : selectedRowKeys) {
			if (!first) script.append(",");
			script.append("'").append(key).append("'");
			first = false;
		}
		script.append("];");
		
		// Update checkboxes for currently visible rows
		script.append("    widget.tbody.children('tr').each(function() {");
		script.append("      var $row = $(this);");
		script.append("      var rowKey = $row.data('rk');");
		script.append("      var $checkbox = $row.find('td.ui-selection-column .ui-chkbox-box');");
		script.append("      if (rowKey && widget.selection.indexOf(rowKey) >= 0) {");
		script.append("        $checkbox.addClass('ui-state-active').children('span').addClass('ui-icon-check');");
		script.append("        $row.addClass('ui-state-highlight').attr('aria-selected', true);");
		script.append("      }");
		script.append("    });");
		script.append("  }");
		script.append("}, 100);");
		
		String jsScript = script.toString();
		Ivy.log().info("Executing client-side script to update checkboxes");
		PrimeFaces.current().executeScript(jsScript);
		
		// Clean up
		context.getExternalContext().getRequestMap().remove(var);
	}
	
	/**
	 * Gets the widgetVar for the current table from component attributes
	 */
	private String getWidgetVar() {
		Object widgetVar = Attrs.currentContext().get("widgetVar");
		return widgetVar != null ? widgetVar.toString() : "dataTable";
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
		mapper.registerModule(new JavaTimeModule())
			.addMixIn(FilterMeta.class, FilterDataTableMixin.class)
			.addMixIn(SortMeta.class, SortDataTableMixin.class);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// Write dates as ISO-8601 strings (e.g., "2025-10-01") instead of arrays
		mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

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
		mapper.registerModule(new JavaTimeModule())
			.addMixIn(FilterMeta.class, FilterDataTableMixin.class)
			.addMixIn(SortMeta.class, SortDataTableMixin.class);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// Read dates from ISO-8601 strings
		mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		
		DataTableState tableState = null;

		try {
			tableState = mapper.readValue(stateJson, new TypeReference<DataTableState>() {
			});
		} catch (IOException e) {
			Ivy.log().error("Couldn't deserialize TableState from JSON", e);
		}

		return tableState;
	}

	/**
	 * Converts date string values in FilterMeta back to LocalDate objects.
	 * After deserialization, date values are strings (e.g., "2025-10-19"),
	 * but PrimeFaces filtering expects LocalDate objects for proper comparison.
	 * 
	 * @param state The DataTableState containing filter metadata
	 */
	private void convertDateStringsToLocalDates(DataTableState state) {
		if (state == null || state.getFilterBy() == null) {
			return;
		}
		
		Map<String, FilterMeta> filterBy = state.getFilterBy();
		for (FilterMeta filterMeta : filterBy.values()) {
			Object filterValue = filterMeta.getFilterValue();
			
			if (filterValue instanceof List) {
				List<?> filterList = (List<?>) filterValue;
				List<Object> convertedList = new ArrayList<>();
				boolean hasChanges = false;
				
				for (Object item : filterList) {
					if (item instanceof String) {
						try {
							// Try to parse as LocalDate (ISO-8601 format: "2025-10-19")
							LocalDate date = LocalDate.parse((String) item);
							convertedList.add(date);
							hasChanges = true;
						} catch (DateTimeParseException e) {
							// Not a date string, keep as is
							convertedList.add(item);
						}
					} else {
						convertedList.add(item);
					}
				}
				
				// Update the filter value if we converted any dates
				if (hasChanges) {
					filterMeta.setFilterValue(convertedList);
				}
			} else if (filterValue instanceof String) {
				try {
					// Try to parse single value as LocalDate
					LocalDate date = LocalDate.parse((String) filterValue);
					filterMeta.setFilterValue(date);
				} catch (DateTimeParseException e) {
					// Not a date string, leave as is
				}
			}
		}
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
