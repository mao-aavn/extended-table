package com.axonivy.market.extendedtable.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.primefaces.PrimeFaces;
import org.primefaces.component.column.Column;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.datatable.DataTableState;
import org.primefaces.component.datepicker.DatePicker;

import ch.ivyteam.ivy.environment.Ivy;

/**
 * Utility class for PrimeFaces DataTable and Column operations.
 * Contains methods for managing table selection, state, and component interactions.
 */
public final class DataTableUtils {

	private DataTableUtils() {
	}

	/**
	 * Clears the selection from both the DataTable and the backing bean's selection.
	 * Works for both single and multiple selection modes.
	 *
	 * @param table The DataTable component
	 */
	public static void clearSelection(DataTable table) {
		// Use ValueExpression to properly clear the backing bean's selection
		// This works for both single selection and multiple selection modes
		ValueExpression ve = table.getValueExpression("selection");
		if (ve != null) {
			FacesContext context = JSFUtils.currentContext();
			Object selectionValue = ve.getValue(context.getELContext());

			if (selectionValue instanceof List) {
				// Multiple selection mode - try to clear the list
				try {
					@SuppressWarnings("unchecked")
					List<Object> selectionList = (List<Object>) selectionValue;
					selectionList.clear();
				} catch (UnsupportedOperationException e) {
					// If the list is immutable, set the value expression to null or empty list
					Ivy.log().warn("Selection list is immutable, setting to null instead");
					ve.setValue(context.getELContext(), null);
				}
			} else if (selectionValue != null) {
				// Single selection mode - set to null
				ve.setValue(context.getELContext(), null);
			}
		}

		// Clear the DataTable's selection property
		table.setSelection(null);

		// Clear the state's selectedRowKeys if state exists
		DataTableState state = table.getMultiViewState(false);
		if (state != null) {
			Set<String> stateSelectedRowKeys = state.getSelectedRowKeys();
			if (stateSelectedRowKeys != null) {
				try {
					stateSelectedRowKeys.clear();
				} catch (UnsupportedOperationException e) {
					Ivy.log().warn("State selectedRowKeys is immutable, cannot clear");
				}
			}
		}

		// Update the table to reflect changes
		updateTable(table);
	}

	/**
	 * Restores the selection by converting row keys back to actual objects.
	 * PrimeFaces stores row keys in the state, but the selection needs to be
	 * populated with the actual objects for proper UI representation.
	 *
	 * @param table           The DataTable component
	 * @param selectedRowKeys The set of row keys from the persisted state
	 * @param widgetVar       The widget variable name for client-side sync (optional)
	 */
	public static void restoreSelection(DataTable table, Set<String> selectedRowKeys, String widgetVar) {
		if (selectedRowKeys == null || selectedRowKeys.isEmpty()) {
			return;
		}

		FacesContext context = JSFUtils.currentContext();

		// Get the value (data source) from the table - use filtered value if available
		Object value = table.getFilteredValue();
		if (value == null || !(value instanceof List)) {
			// Fallback to full value if no filtered value
			value = table.getValue();
		}

		if (!(value instanceof List)) {
			Ivy.log().warn("Table value is not a List. Type: {0}", value != null ? value.getClass().getName() : "null");
			return;
		}

		List<?> items = (List<?>) value;

		// Get the rowKey value expression to extract keys from items
		ValueExpression rowKeyVE = table.getValueExpression("rowKey");
		if (rowKeyVE == null) {
			Ivy.log().warn("No rowKey value expression found on table");
			return;
		}

		String var = table.getVar();

		// Match items by their row keys and collect matched items
		List<Object> matchedItems = new ArrayList<>();
		for (Object item : items) {
			context.getExternalContext().getRequestMap().put(var, item);
			Object rowKey = rowKeyVE.getValue(context.getELContext());

			if (rowKey != null && selectedRowKeys.contains(rowKey.toString())) {
				matchedItems.add(item);
			}
		}

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
				} catch (UnsupportedOperationException e) {
					// If immutable, create new set
					Ivy.log().warn("State selectedRowKeys is immutable, creating new set");
					state.setSelectedRowKeys(new java.util.HashSet<>(selectedRowKeys));
				}
			} else {
				state.setSelectedRowKeys(new java.util.HashSet<>(selectedRowKeys));
			}
		}

		// For checkbox selection, sync the client-side widget using PrimeFaces API
		// Check if this is checkbox selection (no selectionMode attribute means checkbox column)
		String selectionMode = table.getSelectionMode();
		if ((selectionMode == null || selectionMode.isEmpty()) && widgetVar != null) {
			// This is checkbox selection - use the component's JavaScript function
			restoreSelectionOnClient(widgetVar, selectedRowKeys);
		}

		// Update the table component to reflect changes
		updateTable(table);

		// Clean up
		context.getExternalContext().getRequestMap().remove(var);
	}

	/**
	 * Restores selection on the client-side by calling the JavaScript function.
	 * This is used for checkbox selection mode where client-side widget sync is needed.
	 *
	 * @param widgetVar The widget variable name
	 * @param rowKeys   The set of row keys to select
	 */
	public static void restoreSelectionOnClient(String widgetVar, Set<String> rowKeys) {
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
	}

	/**
	 * Updates a DataTable component via AJAX to reflect changes in the UI.
	 * This is commonly used after modifying table state, selection, or data.
	 *
	 * @param table The DataTable component to update
	 */
	public static void updateTable(DataTable table) {
		PrimeFaces.current().ajax().update(table.getClientId());
	}

	/**
	 * Searches for a DatePicker component in the filter facet and extracts its pattern.
	 *
	 * @param column The column to search
	 * @return The date pattern if found, null otherwise
	 */
	public static String findDatePickerPattern(Column column) {
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
	public static String findDatePickerPatternRecursive(UIComponent component) {
		// Check if this is a DatePicker component
		if (component instanceof DatePicker) {
			DatePicker datePicker = (DatePicker) component;
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
	 * Extracts the list of rendered column identifiers from the DataTable. This
	 * captures which columns are currently visible in the table. Uses id as
	 * identifiers.
	 * 
	 * @param table The DataTable component
	 * @return List of column identifiers in render order
	 */
	public static List<String> extractRenderedColumnIds(DataTable table) {
		List<String> renderedColumnIds = new ArrayList<>();

		if (table == null) {
			return renderedColumnIds;
		}

		List<UIComponent> children = table.getChildren();
		if (children == null) {
			return renderedColumnIds;
		}

		for (UIComponent child : children) {
			if (child instanceof Column) {
				Column column = (Column) child;

				// Only process rendered columns
				if (!column.isRendered()) {
					continue;
				}

				String columnId = column.getId();
				if (columnId == null) {
					throw new RuntimeException("Column id should not be null!");
				}

				renderedColumnIds.add(columnId);
			}
		}

		return renderedColumnIds;
	}

}