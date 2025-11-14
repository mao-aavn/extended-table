package com.axonivy.market.extendedtable.beans;

import static com.axonivy.market.extendedtable.utils.DataTableUtils.clearSelection;
import static com.axonivy.market.extendedtable.utils.DataTableUtils.extractRenderedColumnIds;
import static com.axonivy.market.extendedtable.utils.DataTableUtils.findDatePickerPattern;
import static com.axonivy.market.extendedtable.utils.DataTableUtils.restoreSelection;
import static com.axonivy.market.extendedtable.utils.JSFUtils.addErrorMsg;
import static com.axonivy.market.extendedtable.utils.JSFUtils.addInfoMsg;
import static com.axonivy.market.extendedtable.utils.JSFUtils.copyToClipboard;
import static com.axonivy.market.extendedtable.utils.JSFUtils.findComponent;
import static com.axonivy.market.extendedtable.utils.JSFUtils.findComponentFromClientId;
import static com.axonivy.market.extendedtable.utils.JSFUtils.getRequestParam;
import static com.axonivy.market.extendedtable.utils.JSFUtils.getViewRoot;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.primefaces.PrimeFaces;
import org.primefaces.component.column.Column;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.datatable.DataTableState;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;
import org.primefaces.model.filter.FilterConstraint;

import com.axonivy.market.extendedtable.controllers.IvyUserStateController;
import com.axonivy.market.extendedtable.controllers.TableStateController;
import com.axonivy.market.extendedtable.model.CustomDataTableState;
import com.axonivy.market.extendedtable.utils.Attrs;
import com.axonivy.market.extendedtable.utils.JSFUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
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
	private static final String CC_COLUMNS_RENDERED_CALLBACK = "columnsRenderCallback";
	private static final String CC_ATTRS_INITIAL_STATE_NAME = "initialStateName";

	private static final String GROWL_MSG_ID = "extendedTableGrowlMsg";
	private static final String STATE_KEY_PREFIX = "TABLE_STATE_";
	private static final String STATE_KEY_PATTERN = STATE_KEY_PREFIX + "%s_%s";
	private String stateName;
	private boolean initialized = false;

	private static final ObjectMapper TABLE_STATE_MAPPER = createObjectMapperInstance();

	private List<String> stateNames = new ArrayList<>();

	/**
	 * Initialize the component and restore the state if initialStateName is
	 * provided. This method is called via f:event preRenderComponent.
	 */
	public void restoreStateFromGivenInitialStateName() {
		// Prevent multiple initializations (e.g., during AJAX updates)
		if (initialized) {
			return;
		}
		initialized = true;

		// Fetch available saved state names for this table
		stateNames = fetchAllDataTableStateNames();
		if (stateNames == null) {
			stateNames = new ArrayList<>();
		}

		String initialStateName = Attrs.get(CC_ATTRS_INITIAL_STATE_NAME);
		if (initialStateName == null || initialStateName.isEmpty()) {
			addErrorMsg(GROWL_MSG_ID, "initialStateName should be set", null);
			return;
		}

		// Check if the provided initial state exists for this table
		boolean isStateExists = stateNames.stream().anyMatch(name -> name != null && name.equals(initialStateName));
		if (!isStateExists) {
			addErrorMsg(GROWL_MSG_ID, "State not found: " + initialStateName, null);
			return;
		}

		this.stateName = initialStateName;
		try {
			restoreTableState();
		} catch (RuntimeException e) {
			// Catch any unexpected runtime exceptions during restore
			Ivy.log().error("Unexpected error while restoring initial state for table {0}: {1}", getTableClientId(),
					e.getMessage(), e);
			addErrorMsg(GROWL_MSG_ID, "Failed to apply initial table state due to an unexpected error.", null);
		}
	}

	public void saveTableState() {
		String explicitSave = getRequestParam("explicitSave");

		// Ignore in case unexpected first button submission
		if (!"true".equals(explicitSave)) {
			return;
		}

		if (stateName == null || stateName.isEmpty()) {
			addErrorMsg(GROWL_MSG_ID, Attrs.get(CC_ATTRS_STATE_NAME_REQUIRED_MSG), null);
		} else {
			String tableClientId = getTableClientId();
			DataTable table = (DataTable) findComponentFromClientId(tableClientId);

			if (table != null) {
				// Force create the state if it doesn't exist yet
				DataTableState state = table.getMultiViewState(true);

				if (state != null) {
					// Ensure state has current table values even if no user interaction yet
					if (state.getFirst() == 0) {
						state.setFirst(table.getFirst());
					}
					if (state.getRows() == 0) {
						state.setRows(table.getRows());
					}

					persistDataTableState(state);
					stateNames = fetchAllDataTableStateNames();
					addInfoMsg(GROWL_MSG_ID, Attrs.get(CC_ATTRS_SAVE_SUCCESS_MSG), null);
				} else {
					Ivy.log().warn("State is null for the table: {0}", getTableClientId());
				}
			}
		}

	}

	public void restoreTableState() {
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

		CustomDataTableState persistedState = fetchDataTableState();

		if (persistedState != null) {
			// Invoke the callback to notify parent bean about rendered columns
			MethodExpression callback = Attrs.get(CC_COLUMNS_RENDERED_CALLBACK);
			if (callback != null) {
				invokeColumnsRenderedCallback(persistedState.getRenderedColumns(), callback);
			}

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

		currentTable.getExpandedRowKeys().clear();
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
	
	public void copyShareableStateLinkToClipboard() {
		// Build shareable link and copy it to clipboard on the client.
		if (stateName == null || stateName.isEmpty()) {
			addErrorMsg(GROWL_MSG_ID, Attrs.get(CC_ATTRS_STATE_NAME_REQUIRED_MSG), null);
			return;
		}

		try {
			// Try to get a stable base link for the current process start
			String baseLink = null;
			if (Ivy.wfCase().getProcessStart() != null && Ivy.wfCase().getProcessStart().getLink() != null) {
				baseLink = Ivy.wfCase().getProcessStart().getLink().getAbsolute();
			}

			if (baseLink == null || baseLink.isBlank()) {
				addErrorMsg(GROWL_MSG_ID, "Unable to build share link (process start link not available)", null);
				return;
			}

			String encodedState = URLEncoder.encode(stateName, StandardCharsets.UTF_8.toString());
			String sep = baseLink.contains("?") ? "&" : "?";
			String shareLink = baseLink + sep + "embedInFrame&state=" + encodedState;

			copyToClipboard(shareLink);
			addInfoMsg(GROWL_MSG_ID, "Share link copied to clipboard.", null);
		} catch (Exception e) {
			Ivy.log().error("Failed to build or copy share link: {0}", e.getMessage(), e);
			addErrorMsg(GROWL_MSG_ID, "Failed to create share link", null);
		}
	}

	/**
	 * Invokes the columnsRenderCallback if provided by the parent view. This passes
	 * the list of rendered column fields back to the consuming bean.
	 * 
	 * @param renderedColumns The list of column field names that were rendered
	 * @param callback
	 */
	private void invokeColumnsRenderedCallback(List<String> renderedColumns, MethodExpression callback) {
		// Get the callback method expression from component attributes
		if (renderedColumns != null) {
			try {
				FacesContext context = FacesContext.getCurrentInstance();
				callback.invoke(context.getELContext(), new Object[] { renderedColumns });
			} catch (Exception e) {
				Ivy.log().error("Failed to invoke onColumnsRendered callback", e);
			}
		}
	}

	private void persistDataTableState(DataTableState state) {
		String stateKey = getStateKey();
		ObjectMapper mapper = TABLE_STATE_MAPPER;

		// Extract metadata from the table
		DataTable table = (DataTable) JSFUtils.findComponentFromClientId(getTableClientId());
		Map<String, String> columnDateFormats = extractDateFormatsFromColumns(table);

		// Create CustomDataTableState with all necessary information
		CustomDataTableState customState = new CustomDataTableState();

		// Copy base DataTableState properties
		customState.setFilterBy(state.getFilterBy());
		customState.setSortBy(state.getSortBy());
		customState.setFirst(state.getFirst());
		customState.setColumnMeta(state.getColumnMeta());
		customState.setExpandedRowKeys(state.getExpandedRowKeys());
		customState.setSelectedRowKeys(state.getSelectedRowKeys());
		customState.setRows(state.getRows());
		customState.setWidth(state.getWidth());

		// Add custom properties
		customState.setDateFormats(columnDateFormats);

		// Only set rendered columns if callback is provided
		if (Attrs.get(CC_COLUMNS_RENDERED_CALLBACK) != null) {
			List<String> renderedColumnIds = extractRenderedColumnIds(table);
			customState.setRenderedColumns(renderedColumnIds);
		}

		try {
			// Serialize the CustomDataTableState directly
			String stateJson = mapper.writeValueAsString(customState);
			getController().save(stateKey, stateJson);
		} catch (JsonProcessingException e) {
			Ivy.log().error("Couldn't serialize CustomDataTableState to JSON", e);
		}
	}

	private CustomDataTableState fetchDataTableState() {
		String stateKey = getStateKey();
		String stateJson = getController().load(stateKey);

		if (stateJson == null || stateJson.isBlank()) {
			return null;
		}

		ObjectMapper mapper = TABLE_STATE_MAPPER;
		CustomDataTableState tableState = null;

		try {
			// Deserialize directly to CustomDataTableState
			tableState = mapper.readValue(stateJson, CustomDataTableState.class);

			// Convert date strings back to LocalDate objects using the stored format
			// patterns
			if (tableState != null) {
				convertDateStringsUsingFormats(tableState);
			}
		} catch (IOException e) {
			Ivy.log().error("Couldn't deserialize CustomDataTableState from JSON", e);
		}

		return tableState;
	}

	/**
	 * Creates a configured ObjectMapper for serializing/deserializing
	 * DataTableState.
	 * 
	 * <pre>
	 * Default configuration: 
	 * - Includes JavaTimeModule for java.time.* types (LocalDate, LocalDateTime, etc.) 
	 * - Serializes dates as ISO-8601 strings instead of timestamps - Ignores unknown properties during deserialization
	 * 
	 * Users can customize date format by providing a dateFormat attribute to the
	 * ExtendedTable component (e.g., dateFormat="MM/dd/yyyy").
	 * </pre>
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
	private void convertDateStringsUsingFormats(CustomDataTableState state) {
		if (state.getFilterBy() == null || state.getDateFormats() == null || state.getDateFormats().isEmpty()) {
			return;
		}

		Map<String, FilterMeta> filterBy = state.getFilterBy();
		for (Map.Entry<String, FilterMeta> entry : filterBy.entrySet()) {
			FilterMeta filterMeta = entry.getValue();
			Object filterValue = filterMeta.getFilterValue();

			// Check if this field has a date format
			String field = entry.getValue().getField();
			String pattern = state.getDateFormats().get(field);
			if (pattern == null) {
				continue;
			}

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

			// Date range
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

				// Single date
			} else if (filterValue instanceof String str) {
				LocalDate converted = parseLocalDate(str, formatter);
				if (converted != null) {
					filterMeta.setFilterValue(converted);
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
		String safeTableId = getSanitizedClientId();
		String prefix = STATE_KEY_PREFIX + safeTableId + "_";

		return getController().listKeys(prefix).stream().filter(name -> name.startsWith(prefix))
				.map(name -> name.substring(prefix.length())).filter(value -> !value.isEmpty()).toList();
	}

	private String getStateKey() {
		String safeTableId = getSanitizedClientId();

		return String.format(STATE_KEY_PATTERN, safeTableId, stateName);
	}

	private String getSanitizedClientId() {
		String tableId = getTableClientId();
		if (tableId == null) {
			return "";
		}
		// Replace characters that may be problematic in keys
		return tableId.replace(':', '_').replace(' ', '_');
	}

	/**
	 * In case dataTableStateRepository attribute is passed to the ExtendedTable
	 * component, it will be used for persisting the table state, otherwise it will
	 * fallback to the default SessionDataTableStateRepository which persists the
	 * state data to the Ivy User's property map.
	 * 
	 */
	private TableStateController getController() {
		Object stateController = Attrs.get(CC_ATTRS_TABLE_STATE_CONTROLLER);
		if (stateController instanceof TableStateController) {
			return (TableStateController) stateController;
		}

		// Fallback to default in case no overriding controller is set
		return new IvyUserStateController();
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
