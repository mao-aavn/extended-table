package com.axonivy.market.extendedtable.beans;

import static com.axonivy.market.extendedtable.utils.DataTableUtils.clearSelection;
import static com.axonivy.market.extendedtable.utils.DataTableUtils.findDatePickerPattern;
import static com.axonivy.market.extendedtable.utils.DataTableUtils.restoreSelection;
import static com.axonivy.market.extendedtable.utils.JSFUtils.addErrorMsg;
import static com.axonivy.market.extendedtable.utils.JSFUtils.addInfoMsg;
import static com.axonivy.market.extendedtable.utils.JSFUtils.findComponent;
import static com.axonivy.market.extendedtable.utils.JSFUtils.findComponentFromClientId;
import static com.axonivy.market.extendedtable.utils.JSFUtils.getRequestParam;
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

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;

import org.primefaces.PrimeFaces;
import org.primefaces.component.column.Column;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.datatable.DataTableState;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;
import org.primefaces.model.filter.FilterConstraint;

import com.axonivy.market.extendedtable.controllers.IvyUserStateController;
import com.axonivy.market.extendedtable.controllers.TableStateController;
import com.axonivy.market.extendedtable.utils.Attrs;
import com.axonivy.market.extendedtable.utils.JSFUtils;
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

	private static final String CC_ATTRS_TABLE_ID = "tableId";
	private static final String CC_ATTRS_TABLE_STATE_CONTROLLER = "tableStateController";
	private static final String CC_ATTRS_STATE_NAME_REQUIRED_MSG = "stateNameRequiredMsg";
	private static final String CC_ATTRS_SAVE_SUCCESS_MSG = "saveSuccessMsg";
	private static final String CC_ATTRS_WIDGET_VAR = "widgetVar";
	private static final String CC_ATTRS_DELETE_ERROR_MSG = "deleteErrorMsg";
	private static final String CC_ATTRS_DELETE_SUCCESS_MSG = "deleteSuccessMsg";

	private static final String STATE = "state";
	private static final String DATE_FORMATS = "dateFormats";
	private static final String GROWL_MSG_ID = "extendedTableGrowlMsg";
	private static final String STATE_KEY_PREFIX = "DATATABLE_";
	private static final String STATE_KEY_PATTERN = STATE_KEY_PREFIX + "%s_%s";
	private String stateName;
	private List<String> stateNames = new ArrayList<>();

	// Allow tests or integrators to inject a controller to avoid static lookups in Attrs
	private TableStateController controllerOverride;

	private static final ObjectMapper TABLE_STATE_MAPPER = createObjectMapperInstance();

	public void saveTableState() {
		String explicitSave = getRequestParam("explicitSave");

		if (!"true".equals(explicitSave)) {
			return;
		}

		if (stateName == null || stateName.isEmpty()) {
			addErrorMsg(GROWL_MSG_ID, Attrs.get(CC_ATTRS_STATE_NAME_REQUIRED_MSG), null);
		} else {
			String tableClientId = getTableClientId();
			DataTable table = (DataTable) findComponentFromClientId(tableClientId);

			if (table != null) {
				DataTableState state = table.getMultiViewState(false);
				if (state != null) {
					persistDataTableState(state);
					stateNames = fetchAllDataTableStateNames();
					addInfoMsg(GROWL_MSG_ID, Attrs.get(CC_ATTRS_SAVE_SUCCESS_MSG), null);
				} else {
					Ivy.log().warn("State is null for the table: {0}", getTableClientId());
				}
			}
		}
	}

	public void restoreTableState() throws JsonProcessingException {
		if (stateName == null || stateName.isEmpty()) {
			addErrorMsg(GROWL_MSG_ID, Attrs.get(CC_ATTRS_STATE_NAME_REQUIRED_MSG), null);
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
			currentState.setWidth(persistedState.getWidth());
			currentTable.setFirst(persistedState.getFirst());

			// Apply filters and sorting first to get the correct filtered dataset
			currentTable.filterAndSort();
			currentTable.resetColumns();

			// Restore selection AFTER filterAndSort so we work with filtered items
			restoreSelection(currentTable, persistedState.getSelectedRowKeys(), Attrs.get(CC_ATTRS_WIDGET_VAR));
		} else {
			Ivy.log().warn("No saved table state to restore for the table %s and state %s", getTableClientId(),
					stateName);
		}
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

	public void deleteTableState() {
		String stateKey = getStateKey();
		if (!getController().delete(stateKey)) {
			addErrorMsg(GROWL_MSG_ID, Attrs.get(CC_ATTRS_DELETE_ERROR_MSG), null);
		} else {
			stateNames = fetchAllDataTableStateNames();
			if (stateNames.size() > 0) {
				stateName = stateNames.get(0);
			} else {
				stateName = null;
			}
			addInfoMsg(GROWL_MSG_ID, Attrs.get(CC_ATTRS_DELETE_SUCCESS_MSG), null);
		}
	}

	public List<String> completeStateName(String query) {
		if (stateNames == null || stateNames.isEmpty()) {
			stateNames = fetchAllDataTableStateNames();
		}
		if (stateNames == null || stateNames.isEmpty()) {
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

		ObjectMapper mapper = TABLE_STATE_MAPPER;

		// Extract and store date format patterns from columns before serialization
		DataTable table = (DataTable) JSFUtils.findComponentFromClientId(getTableClientId());
		Map<String, String> columnDateFormats = extractDateFormatsFromColumns(table);

		try {
			// Create a wrapper object to store both state and format info
			Map<String, Object> stateWrapper = new HashMap<>();
			stateWrapper.put(STATE, state);
			stateWrapper.put(DATE_FORMATS, columnDateFormats);

			String stateJson = mapper.writeValueAsString(stateWrapper);
			getController().save(stateKey, stateJson);
		} catch (JsonProcessingException e) {
			Ivy.log().error("Couldn't serialize TableState to JSON", e);
		}
	}

	private DataTableState fetchDataTableState() {
		String stateKey = getStateKey();
		String stateJson = getController().load(stateKey);

		ObjectMapper mapper = TABLE_STATE_MAPPER;

		DataTableState tableState = null;

		try {
			// Validate and read the wrapper containing both state and date formats
			if (stateJson == null || stateJson.isBlank()) {
				return null;
			}
			Map<String, Object> stateWrapper = mapper.readValue(stateJson, new TypeReference<Map<String, Object>>() {
			});

			// Extract the state
			Object stateObj = stateWrapper.get(STATE);
			if (stateObj != null) {
				// Convert the state object back to DataTableState
				String stateJsonStr = mapper.writeValueAsString(stateObj);
				tableState = mapper.readValue(stateJsonStr, new TypeReference<DataTableState>() {
				});

				// Extract and apply date formats to convert string dates back to proper types
				@SuppressWarnings("unchecked")
				Map<String, String> dateFormats = (Map<String, String>) stateWrapper.get(DATE_FORMATS);
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
	private static ObjectMapper createObjectMapperInstance() {
		ObjectMapper mapper = new ObjectMapper();

		// Register JavaTimeModule for standard java.time types
		mapper.registerModule(new JavaTimeModule());

		// Apply mixins to exclude problematic fields from serialization
		mapper.addMixIn(FilterMeta.class, FilterDataTableMixin.class);
		mapper.addMixIn(SortMeta.class, SortDataTableMixin.class);

		// Configure behavior
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
		List<UIComponent> children = table.getChildren();
		if (children == null) {
			return dateFormats;
		}

		for (UIComponent child : children) {
			if (child instanceof Column) {
				Column column = (Column) child;
				String field = column.getField();

				if (field != null && !field.isEmpty()) {
					String pattern = findDatePickerPattern(column);

					if (pattern != null) {
						dateFormats.put(field, pattern);
					}
				}
			}
		}

		return dateFormats;
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
		String tableId = getTableClientId();
		String safeTableId = sanitizeClientId(tableId);
		return String.format(STATE_KEY_PATTERN, safeTableId, stateName);
	}

	private String sanitizeClientId(String id) {
		if (id == null) {
			return "";
		}
		// Replace characters that may be problematic in keys
		return id.replace(':', '_').replace(' ', '_');
	}

	/**
	 * In case dataTableStateRepository attribute is passed to the ExtendedTable
	 * component, it will be used for persisting the table state, otherwise it will
	 * fallback to the default SessionDataTableStateRepository which persists the
	 * state data to the Ivy User's property map.
	 * 
	 */
	private TableStateController getController() {
		// If a test or integrator injected a controller, use it
		if (controllerOverride != null) {
			return controllerOverride;
		}

		Object repoObj = Attrs.get(CC_ATTRS_TABLE_STATE_CONTROLLER);
		if (repoObj instanceof TableStateController) {
			return (TableStateController) repoObj;
		}

		// Fallback to default in case no overriding controller is set
		return new IvyUserStateController();
	}

	/**
	 * Setter allowing injection of a TableStateController for testing or
	 * integration. This reduces the need for static mocking of Attrs in tests.
	 */
	public void setControllerOverride(TableStateController controller) {
		this.controllerOverride = controller;
	}

	private String getTableClientId() {
		UIComponent tableComponent = findComponent((String) Attrs.get(CC_ATTRS_TABLE_ID));

		if (tableComponent == null) {
			throw new IllegalStateException(
					"Component with id '" + Attrs.get(CC_ATTRS_TABLE_ID) + "' not found in view.");
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
