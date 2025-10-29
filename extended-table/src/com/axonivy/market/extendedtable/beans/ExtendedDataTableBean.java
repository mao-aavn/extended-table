package com.axonivy.market.extendedtable.beans;

import static com.axonivy.market.extendedtable.utils.JSFUtils.addErrorMsg;
import static com.axonivy.market.extendedtable.utils.JSFUtils.addInfoMsg;
import static com.axonivy.market.extendedtable.utils.JSFUtils.findComponent;
import static com.axonivy.market.extendedtable.utils.JSFUtils.findComponentFromClientId;
import static com.axonivy.market.extendedtable.utils.JSFUtils.getViewRoot;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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

	public void restoreTableState() throws JsonProcessingException {
		if (stateName == null || stateName.isEmpty()) {
			addErrorMsg(GROWL_MSG_ID, "State name is required!", null);
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
		DataTableState state = table.getMultiViewState(true); // force create to ensure we have a state
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
		script.append("  if (widget) {");
		
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
	}
	
	/**
	 * Clears the selection from both the DataTable and the backing bean's selection list
	 * 
	 * @param table The DataTable component
	 */
	private void clearSelection(DataTable table) {
		Ivy.log().info("clearSelection called");
		
		// Clear the backing bean's selection list FIRST (from composite component attribute)
		// This is the most important step as it's bound to the UI
		Object selectionAttr = Attrs.currentContext().get("selection");
		if (selectionAttr instanceof List) {
			@SuppressWarnings("unchecked")
			List<Object> selectionList = (List<Object>) selectionAttr;
			selectionList.clear();
			Ivy.log().info("Cleared selection list from backing bean, size: {0}", selectionList.size());
		}
		
		// Clear the DataTable's selection property
		table.setSelection(null);
		Ivy.log().info("Set DataTable selection to null");
		
		// Clear the state's selectedRowKeys if state exists
		DataTableState state = table.getMultiViewState(false);
		if (state != null) {
			Set<String> stateSelectedRowKeys = state.getSelectedRowKeys();
			if (stateSelectedRowKeys != null) {
				stateSelectedRowKeys.clear();
				Ivy.log().info("Cleared state selectedRowKeys");
			}
		}
		
		// Update the table to reflect changes
		PrimeFaces.current().ajax().update(table.getClientId());
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
			Map<String, Object> stateWrapper = mapper.readValue(stateJson, new TypeReference<Map<String, Object>>() {});
			
			// Extract the state
			Object stateObj = stateWrapper.get("state");
			if (stateObj != null) {
				// Convert the state object back to DataTableState
				String stateJsonStr = mapper.writeValueAsString(stateObj);
				tableState = mapper.readValue(stateJsonStr, new TypeReference<DataTableState>() {});
				
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
	 * Creates a configured ObjectMapper for serializing/deserializing DataTableState.
	 * 
	 * Default configuration:
	 * - Includes JavaTimeModule for java.time.* types (LocalDate, LocalDateTime, etc.)
	 * - Serializes dates as ISO-8601 strings instead of timestamps
	 * - Ignores unknown properties during deserialization
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
		mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		
		// Apply custom date format if provided (global fallback)
		String dateFormatPattern = (String) Attrs.currentContext().get("dateFormat");
		if (dateFormatPattern != null && !dateFormatPattern.trim().isEmpty()) {
			java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat(dateFormatPattern);
			mapper.setDateFormat(dateFormat);
		}
		
		return mapper;
	}

	/**
	 * Extracts date format patterns from DateTimeConverter components in table columns.
	 * This allows the component to automatically use the same format defined in the UI.
	 * 
	 * @param table The DataTable component
	 * @return Map of column field names to date format patterns
	 */
	private Map<String, String> extractDateFormatsFromColumns(DataTable table) {
		Map<String, String> dateFormats = new HashMap<>();
		
		if (table == null) {
			return dateFormats;
		}
		
		// Iterate through columns to find DateTimeConverter components or DatePicker patterns
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
	 * Searches for a DatePicker component in the filter facet and extracts its pattern.
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
			org.primefaces.component.datepicker.DatePicker datePicker = 
				(org.primefaces.component.datepicker.DatePicker) component;
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
	 * Recursively searches for a DateTimeConverter in the component tree and extracts its pattern.
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
	 * @param state The DataTableState containing filter metadata
	 * @param dateFormats Map of column field names to date format patterns
	 */
	private void convertDateStringsUsingFormats(DataTableState state, Map<String, String> dateFormats) {
		if (state == null || state.getFilterBy() == null || dateFormats == null || dateFormats.isEmpty()) {
			return;
		}
		
		Map<String, FilterMeta> filterBy = state.getFilterBy();
		for (Map.Entry<String, FilterMeta> entry : filterBy.entrySet()) {
			String field = entry.getKey();
			FilterMeta filterMeta = entry.getValue();
			Object filterValue = filterMeta.getFilterValue();
			
			// Check if this field has a date format
			String pattern = dateFormats.get(field);
			if (pattern == null) {
				continue;
			}
			
			SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
			
			if (filterValue instanceof List) {
				List<?> filterList = (List<?>) filterValue;
				List<Object> convertedList = new ArrayList<>();
				
				for (Object item : filterList) {
					if (item instanceof String) {
						Object converted = parseDate((String) item, pattern, dateFormat);
						convertedList.add(converted != null ? converted : item);
					} else {
						convertedList.add(item);
					}
				}
				
				filterMeta.setFilterValue(convertedList);
			} else if (filterValue instanceof String) {
				Object converted = parseDate((String) filterValue, pattern, dateFormat);
				if (converted != null) {
					filterMeta.setFilterValue(converted);
				}
			}
		}
	}

	/**
	 * Parses a date string using the given pattern, attempting multiple date types.
	 * 
	 * @param dateStr The date string to parse
	 * @param pattern The date format pattern
	 * @param dateFormat SimpleDateFormat configured with the pattern
	 * @return Parsed date object (Date, LocalDate, or LocalDateTime) or null if parsing fails
	 */
	private Object parseDate(String dateStr, String pattern, SimpleDateFormat dateFormat) {
		try {
			// Check if pattern includes time components
			boolean hasTime = pattern.contains("H") || pattern.contains("h") || 
							  pattern.contains("m") || pattern.contains("s");
			
			// Try parsing as ISO format first (from JSON serialization)
			try {
				if (hasTime) {
					// Try ISO LocalDateTime format (e.g., "2025-10-13T10:49:00")
					return java.time.LocalDateTime.parse(dateStr);
				} else {
					// Try ISO LocalDate format (e.g., "2025-10-06")
					return java.time.LocalDate.parse(dateStr);
				}
			} catch (java.time.format.DateTimeParseException e) {
				// Not ISO format, continue to try custom pattern
			}
			
			// Try parsing with the custom pattern
			Date date = dateFormat.parse(dateStr);
			
			// Convert to appropriate java.time type for better compatibility
			if (hasTime) {
				return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			} else {
				return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			}
		} catch (ParseException e) {
			Ivy.log().warn("Failed to parse date string: {0} with pattern: {1}", dateStr, pattern);
			return null;
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
