package com.axonivy.market.extendedtable.beans;

import static com.axonivy.market.extendedtable.utils.JSFUtils.addErrorMsg;
import static com.axonivy.market.extendedtable.utils.JSFUtils.addInfoMsg;
import static com.axonivy.market.extendedtable.utils.JSFUtils.findComponent;
import static com.axonivy.market.extendedtable.utils.JSFUtils.findComponentFromClientId;
import static com.axonivy.market.extendedtable.utils.JSFUtils.getViewRoot;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.DateTimeConverter;

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
import com.fasterxml.jackson.databind.SerializationFeature;
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
		// Only save if explicitly triggered by the Save button via the explicitSave
		// parameter
		// This prevents accidental saves when filtering or other actions trigger form
		// submission
		String explicitSave = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
				.get("explicitSave");

		if (!"true".equals(explicitSave)) {
			return;
		}

		if (stateName == null || stateName.isEmpty()) {
			addErrorMsg(GROWL_MSG_ID, Attrs.get("stateNameRequiredMsg"), null);
		} else {
			String tableClientId = getTableClientId();
			DataTable table = (DataTable) findComponentFromClientId(tableClientId);

			if (table != null) {
				DataTableState state = table.getMultiViewState(false);
				if (state != null) {
					persistDataTableState(state);
					stateNames = fetchAllDataTableStateNames();
					addInfoMsg(GROWL_MSG_ID, Attrs.get("saveSuccessMsg"), null);
				} else {
					Ivy.log().warn("State is null for the table: {0}", getTableClientId());
				}
			}
		}
	}

	public void restoreTableState() throws JsonProcessingException {
		if (stateName == null || stateName.isEmpty()) {
			addErrorMsg(GROWL_MSG_ID, Attrs.get("stateNameRequiredMsg"), null);
			return;
		}

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
			// Don't set selectedRowKeys here - filterAndSort() will clear them
			// We'll restore selection after filtering in restoreSelection()
			currentState.setWidth(persistedState.getWidth());
			currentTable.setFirst(persistedState.getFirst());

			// Apply filters and sorting first to get the correct filtered dataset
			currentTable.filterAndSort();
			currentTable.resetColumns();
			// currentState.setSelectedRowKeys(currentState.getSelectedRowKeys());

			// Restore selection AFTER filterAndSort so we work with filtered items
			restoreSelection(currentTable, persistedState.getSelectedRowKeys());
		} else {
			Ivy.log().warn("No saved table state to restore for the table %s and state %s", getTableClientId(),
					stateName);
		}
	}

	/**
	 * Restores the selection by converting row keys back to actual objects.
	 * PrimeFaces stores row keys in the state, but the selection needs to be
	 * populated with the actual objects for proper UI representation.
	 * 
	 * @param table           The DataTable component
	 * @param selectedRowKeys The set of row keys from the persisted state
	 */
	private void restoreSelection(DataTable table, Set<String> selectedRowKeys) {
		Ivy.log().info("restoreSelection called with selectedRowKeys: {0}", selectedRowKeys);

		if (selectedRowKeys == null || selectedRowKeys.isEmpty()) {
			Ivy.log().info("No selected row keys to restore");
			return;
		}

		FacesContext context = FacesContext.getCurrentInstance();

		// Get the value (data source) from the table - use filtered value if available
		Object value = table.getFilteredValue();
		if (value == null || !(value instanceof List)) {
			// Fallback to full value if no filtered value
			value = table.getValue();
		}

		Ivy.log().info("Using filtered value: {0}", table.getFilteredValue() != null);

		if (!(value instanceof List)) {
			Ivy.log().warn("Table value is not a List. Type: {0}", value != null ? value.getClass().getName() : "null");
			return;
		}

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

		// Match items by their row keys and collect matched items
		List<Object> matchedItems = new ArrayList<>();
		for (Object item : items) {
			context.getExternalContext().getRequestMap().put(var, item);
			Object rowKey = rowKeyVE.getValue(context.getELContext());

			if (rowKey != null && selectedRowKeys.contains(rowKey.toString())) {
				matchedItems.add(item);
				Ivy.log().info("Matched item with rowKey: {0}", rowKey);
			}
		}

		Ivy.log().info("Restored {0} selected items out of {1} row keys", matchedItems.size(), selectedRowKeys.size());

		// Get the selection ValueExpression and update the backing bean
		ValueExpression selectionVE = table.getValueExpression("selection");
		if (selectionVE != null) {
			Object currentSelection = selectionVE.getValue(context.getELContext());

			if (currentSelection instanceof List) {
				// Multiple selection mode - update the list
				@SuppressWarnings("unchecked")
				List<Object> selectionList = (List<Object>) currentSelection;
				try {
					selectionList.clear();
					selectionList.addAll(matchedItems);
					Ivy.log().info("Updated selection list in backing bean, size: {0}", selectionList.size());
				} catch (UnsupportedOperationException e) {
					// If the list is immutable, set a new list via ValueExpression
					Ivy.log().warn("Selection list is immutable, setting new list");
					selectionVE.setValue(context.getELContext(), new ArrayList<>(matchedItems));
				}

				// Set the selection on the DataTable
				table.setSelection(matchedItems);
			} else {
				// Single selection mode - set the first matched item
				Object selectedItem = matchedItems.isEmpty() ? null : matchedItems.get(0);
				selectionVE.setValue(context.getELContext(), selectedItem);
				table.setSelection(selectedItem);
				Ivy.log().info("Set single selection in backing bean: {0}", selectedItem);
			}
		} else {
			Ivy.log().warn("No selection ValueExpression found on table");
		}

		// Update the state's selectedRowKeys for proper checkbox/row highlighting
		DataTableState state = table.getMultiViewState(true);
		if (state != null) {
			Set<String> stateSelectedRowKeys = state.getSelectedRowKeys();

			if (stateSelectedRowKeys != null) {
				try {
					stateSelectedRowKeys.clear();
					stateSelectedRowKeys.addAll(selectedRowKeys);
					Ivy.log().info("Updated state selectedRowKeys, size: {0}", stateSelectedRowKeys.size());
				} catch (UnsupportedOperationException e) {
					// If immutable, create new set
					Ivy.log().warn("State selectedRowKeys is immutable, creating new set");
					state.setSelectedRowKeys(new java.util.HashSet<>(selectedRowKeys));
				}
			} else {
				state.setSelectedRowKeys(new java.util.HashSet<>(selectedRowKeys));
				Ivy.log().info("Created new selectedRowKeys set with size: {0}", selectedRowKeys.size());
			}
		}

		// For checkbox selection, sync the client-side widget using PrimeFaces API
		// Check if this is checkbox selection (no selectionMode attribute means
		// checkbox column)
		String selectionMode = table.getSelectionMode();
		if (selectionMode == null || selectionMode.isEmpty()) {
			// This is checkbox selection - use the component's JavaScript function
			restoreSelectionOnClient(getWidgetVar(), selectedRowKeys);
		}

		// Update the table component to reflect changes
		PrimeFaces.current().ajax().update(table.getClientId());

		// Clean up
		context.getExternalContext().getRequestMap().remove(var);
	}

	/**
	 * Restores selection on the client-side by calling the JavaScript function.
	 * This is used for checkbox selection mode where client-side widget sync is
	 * needed.
	 * 
	 * @param widgetVar The widget variable name
	 * @param rowKeys   The set of row keys to select
	 */
	private void restoreSelectionOnClient(String widgetVar, Set<String> rowKeys) {
		if (rowKeys == null || rowKeys.isEmpty()) {
			return;
		}

		// Build the JavaScript array of row keys
		StringBuilder keysArray = new StringBuilder("[");
		boolean first = true;
		for (String key : rowKeys) {
			if (!first) {
				keysArray.append(",");
			}
			keysArray.append("'").append(key.replace("'", "\\'")).append("'");
			first = false;
		}
		keysArray.append("]");

		// Call the JavaScript function defined in the component
		String script = String.format("restoreTableSelection('%s', %s);", widgetVar, keysArray.toString());
		PrimeFaces.current().executeScript(script);

		Ivy.log().info("Executed client-side selection restore for widget: {0} with {1} keys", widgetVar,
				rowKeys.size());
	}

	/**
	 * Gets the widgetVar for the current table from component attributes
	 */
	private String getWidgetVar() {
		return Attrs.get("widgetVar");
	}

	public void resetTable() {
		String viewId = getViewRoot().getViewId();
		String tableClientId = getTableClientId();
		DataTable currentTable = (DataTable) findComponentFromClientId(tableClientId);

		if (currentTable == null) {
			Ivy.log().warn("Table not found with the given id: {0}", tableClientId);
			return;
		}

		// Clear selection BEFORE clearing state to ensure clean reset
		clearSelection(currentTable);

		// Now clear the PrimeFaces multiview state
		PrimeFaces.current().multiViewState().clearAll(viewId, true, null);
		stateName = null;

		currentTable.clearInitialState();
		currentTable.resetColumns();

		Ivy.log().warn("RESET COMPLETED");
	}

	/**
	 * Clears the selection from both the DataTable and the backing bean's selection
	 * 
	 * @param table The DataTable component
	 */
	private void clearSelection(DataTable table) {
		Ivy.log().info("clearSelection called");

		// Use ValueExpression to properly clear the backing bean's selection
		// This works for both single selection and multiple selection modes
		ValueExpression ve = table.getValueExpression("selection");
		if (ve != null) {
			FacesContext context = FacesContext.getCurrentInstance();
			Object selectionValue = ve.getValue(context.getELContext());

			if (selectionValue instanceof List) {
				// Multiple selection mode - try to clear the list
				try {
					@SuppressWarnings("unchecked")
					List<Object> selectionList = (List<Object>) selectionValue;
					selectionList.clear();
					Ivy.log().info("Cleared selection list from backing bean, size: {0}", selectionList.size());
				} catch (UnsupportedOperationException e) {
					// If the list is immutable, set the value expression to null or empty list
					Ivy.log().warn("Selection list is immutable, setting to null instead");
					ve.setValue(context.getELContext(), null);
				}
			} else if (selectionValue != null) {
				// Single selection mode - set to null
				ve.setValue(context.getELContext(), null);
				Ivy.log().info("Cleared single selection from backing bean");
			}
		}

		// Clear the DataTable's selection property
		table.setSelection(null);
		Ivy.log().info("Set DataTable selection to null");

		// Clear the state's selectedRowKeys if state exists
		DataTableState state = table.getMultiViewState(false);
		if (state != null) {
			Set<String> stateSelectedRowKeys = state.getSelectedRowKeys();
			if (stateSelectedRowKeys != null) {
				try {
					stateSelectedRowKeys.clear();
					Ivy.log().info("Cleared state selectedRowKeys");
				} catch (UnsupportedOperationException e) {
					Ivy.log().warn("State selectedRowKeys is immutable, cannot clear");
				}
			}
		}

		// Update the table to reflect changes
		PrimeFaces.current().ajax().update(table.getClientId());
	}

	public void deleteTableState() {
		String stateKey = getStateKey();
		if (!getController().delete(stateKey)) {
			addErrorMsg(GROWL_MSG_ID, Attrs.get("deleteErrorMsg"), null);
		} else {
			stateNames = fetchAllDataTableStateNames();
			if (stateNames.size() > 0) {
				stateName = stateNames.get(0);
			} else {
				stateName = null;
			}
			addInfoMsg(GROWL_MSG_ID, Attrs.get("deleteSuccessMsg"), null);
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

		ObjectMapper mapper = createObjectMapper();

		// Extract and store date format patterns from columns before serialization
		DataTable table = (DataTable) findComponentFromClientId(getTableClientId());
		Map<String, String> columnDateFormats = extractDateFormatsFromColumns(table);

		try {
			// Create a wrapper object to store both state and format info
			Map<String, Object> stateWrapper = new HashMap<>();
			stateWrapper.put("state", state);
			stateWrapper.put("dateFormats", columnDateFormats);

			String stateJson = mapper.writeValueAsString(stateWrapper);
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

		ObjectMapper mapper = createObjectMapper();

		DataTableState tableState = null;

		try {
			// Read the wrapper containing both state and date formats
			Map<String, Object> stateWrapper = mapper.readValue(stateJson, new TypeReference<Map<String, Object>>() {
			});

			// Extract the state
			Object stateObj = stateWrapper.get("state");
			if (stateObj != null) {
				// Convert the state object back to DataTableState
				String stateJsonStr = mapper.writeValueAsString(stateObj);
				tableState = mapper.readValue(stateJsonStr, new TypeReference<DataTableState>() {
				});

				// Extract and apply date formats to convert string dates back to proper types
				@SuppressWarnings("unchecked")
				Map<String, String> dateFormats = (Map<String, String>) stateWrapper.get("dateFormats");
				if (dateFormats != null && !dateFormats.isEmpty()) {
					convertDateStringsUsingFormats(tableState, dateFormats);
				}
			}
		} catch (IOException e) {
			Ivy.log().error("Couldn't deserialize TableState from JSON", e);
		}

		return tableState;
	}

	/**
	 * Creates a configured ObjectMapper for serializing/deserializing
	 * DataTableState.
	 * 
	 * Default configuration: - Includes JavaTimeModule for java.time.* types
	 * (LocalDate, LocalDateTime, etc.) - Serializes dates as ISO-8601 strings
	 * instead of timestamps - Ignores unknown properties during deserialization
	 * 
	 * Users can customize date format by providing a dateFormat attribute to the
	 * ExtendedTable component (e.g., dateFormat="MM/dd/yyyy").
	 * 
	 * @return Configured ObjectMapper instance
	 */
	private ObjectMapper createObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();

		// Register JavaTimeModule for standard java.time types
		mapper.registerModule(new JavaTimeModule());

		// Apply mixins to exclude problematic fields from serialization
		mapper.addMixIn(FilterMeta.class, FilterDataTableMixin.class);
		mapper.addMixIn(SortMeta.class, SortDataTableMixin.class);

		// Configure behavior
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
		// false);
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		return mapper;
	}

	/**
	 * Extracts date format patterns from DateTimeConverter components in table
	 * columns. This allows the component to automatically use the same format
	 * defined in the UI.
	 * 
	 * @param table The DataTable component
	 * @return Map of column field names to date format patterns
	 */
	private Map<String, String> extractDateFormatsFromColumns(DataTable table) {
		Map<String, String> dateFormats = new HashMap<>();

		if (table == null) {
			return dateFormats;
		}

		// Iterate through columns to find DateTimeConverter components or DatePicker
		// patterns
		for (UIComponent child : table.getChildren()) {
			if (child instanceof org.primefaces.component.column.Column) {
				org.primefaces.component.column.Column column = (org.primefaces.component.column.Column) child;
				String field = column.getField();

				if (field != null && !field.isEmpty()) {
					// First, check filter facet for DatePicker pattern
					String pattern = findDatePickerPattern(column);

					// If not found in filter facet, search for DateTimeConverter in column children
					if (pattern == null) {
						pattern = findDateTimeConverterPattern(column);
					}

					if (pattern != null) {
						dateFormats.put(field, pattern);
						Ivy.log().info("Found date format for column {0}: {1}", field, pattern);
					}
				}
			}
		}

		return dateFormats;
	}

	/**
	 * Searches for a DatePicker component in the filter facet and extracts its
	 * pattern.
	 * 
	 * @param column The column to search
	 * @return The date pattern if found, null otherwise
	 */
	private String findDatePickerPattern(org.primefaces.component.column.Column column) {
		UIComponent filterFacet = column.getFacet("filter");
		if (filterFacet != null) {
			return findDatePickerPatternRecursive(filterFacet);
		}
		return null;
	}

	/**
	 * Recursively searches for a DatePicker component and extracts its pattern.
	 * 
	 * @param component The component to search
	 * @return The date pattern if found, null otherwise
	 */
	private String findDatePickerPatternRecursive(UIComponent component) {
		// Check if this is a DatePicker component
		if (component instanceof org.primefaces.component.datepicker.DatePicker) {
			org.primefaces.component.datepicker.DatePicker datePicker = (org.primefaces.component.datepicker.DatePicker) component;
			String pattern = datePicker.getPattern();
			if (pattern != null && !pattern.isEmpty()) {
				return pattern;
			}
		}

		// Recursively search children
		for (UIComponent child : component.getChildren()) {
			String pattern = findDatePickerPatternRecursive(child);
			if (pattern != null) {
				return pattern;
			}
		}

		return null;
	}

	/**
	 * Recursively searches for a DateTimeConverter in the component tree and
	 * extracts its pattern.
	 * 
	 * @param component The component to search
	 * @return The date pattern if found, null otherwise
	 */
	private String findDateTimeConverterPattern(UIComponent component) {
		// Check direct converter
		if (component instanceof javax.faces.component.ValueHolder) {
			javax.faces.convert.Converter converter = ((javax.faces.component.ValueHolder) component).getConverter();
			if (converter instanceof DateTimeConverter) {
				DateTimeConverter dtConverter = (DateTimeConverter) converter;
				String pattern = dtConverter.getPattern();
				if (pattern != null && !pattern.isEmpty()) {
					return pattern;
				}
			}
		}

		// Recursively search children
		for (UIComponent child : component.getChildren()) {
			String pattern = findDateTimeConverterPattern(child);
			if (pattern != null) {
				return pattern;
			}
		}

		return null;
	}

	/**
	 * Converts date string values in FilterMeta back to appropriate date objects
	 * using the format patterns extracted from column converters.
	 * 
	 * @param state       The DataTableState containing filter metadata
	 * @param dateFormats Map of column field names to date format patterns
	 */
	private void convertDateStringsUsingFormats(DataTableState state, Map<String, String> dateFormats) {
		if (state == null || state.getFilterBy() == null || dateFormats == null || dateFormats.isEmpty()) {
			return;
		}

		Map<String, FilterMeta> filterBy = state.getFilterBy();
		for (Map.Entry<String, FilterMeta> entry : filterBy.entrySet()) {
			FilterMeta filterMeta = entry.getValue();
			Object filterValue = filterMeta.getFilterValue();

			// Check if this field has a date format
			String field = entry.getValue().getField();
			String pattern = dateFormats.get(field);
			if (pattern == null) {
				continue;
			}

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

			if (filterValue instanceof List) {
				List<?> filterList = (List<?>) filterValue;
				List<LocalDate> convertedList = new ArrayList<>();

				for (Object item : filterList) {
					if (item instanceof String str) {
						LocalDate converted = parseLocalDate(str, formatter);
						if (converted != null) {
							convertedList.add(converted);
						}
					} else if (item instanceof LocalDate date) {
						convertedList.add(date);
					}
				}

				// Only set if we got something valid
				if (!convertedList.isEmpty()) {
					filterMeta.setFilterValue(convertedList);
				}

			} else if (filterValue instanceof String str) {
				LocalDate converted = parseLocalDate(str, formatter);
				if (converted != null) {
					filterMeta.setFilterValue(List.of(converted));
				}
			}
		}
	}

	private LocalDate parseLocalDate(String value, DateTimeFormatter formatter) {
		if (value == null || value.isBlank()) {
			return null;
		}

		try {
			return LocalDate.parse(value, formatter);
		} catch (DateTimeParseException e1) {
			try {
				// Fallback to ISO format (e.g. "2025-10-20")
				return LocalDate.parse(value);
			} catch (DateTimeParseException e2) {
				return null;
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
	 * fallback to the default SessionDataTableStateRepository which persists the
	 * state data to the Ivy User's property map.
	 * 
	 */
	private ExtendedDataTableController getController() {
		ExtendedDataTableController repo = (ExtendedDataTableController) Attrs.get(DATA_TABLE_STATE_REPOSITORY);
		if (repo != null && repo instanceof ExtendedDataTableController) {
			return (ExtendedDataTableController) repo;
		}

		// Fallback to default in case no overriding controller is set
		return new IvySessionExtendedDataTableController();
	}

	private String getTableClientId() {
		UIComponent tableComponent = findComponent((String) Attrs.get(TABLE_ID));

		if (tableComponent == null) {
			throw new IllegalStateException("Component with id '" + Attrs.get(TABLE_ID) + "' not found in view.");
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
