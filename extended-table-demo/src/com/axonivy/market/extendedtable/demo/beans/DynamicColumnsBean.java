package com.axonivy.market.extendedtable.demo.beans;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.faces.model.SelectItem;
import com.axonivy.market.extendedtable.demo.entities.Customer;
import com.axonivy.market.extendedtable.demo.entities.CustomerStatus;

public class DynamicColumnsBean extends GenericDemoBean {

	public enum FilterType {
		TEXT, NUMBER, DATE, DATETIME, ENUM, BOOLEAN, OBJECT
	}

	public static class ColumnDef {
		public String header;
		public String property;
		public FilterType filterType;
		public Class<?> fieldType;

		public ColumnDef(String header, String property, FilterType filterType, Class<?> fieldType) {
			this.header = header;
			this.property = property;
			this.filterType = filterType;
			this.fieldType = fieldType;
		}

		public String getHeader() {
			return header;
		}

		public String getProperty() {
			return property;
		}

		public FilterType getFilterType() {
			return filterType;
		}

		public Class<?> getFieldType() {
			return fieldType;
		}
	}

	private List<ColumnDef> dynamicColumns = new ArrayList<>();

	/** Currently visible column property names */
	private List<String> selectedColumnProperties = new ArrayList<>();

	/** Date range filters for DATE columns - now using LocalDate[] for range picker */
	private Map<String, LocalDate[]> dateFilters = new HashMap<>();

	/** DateTime range filters for DATETIME columns - now using LocalDateTime[] for range picker */
	private Map<String, LocalDateTime[]> dateTimeFilters = new HashMap<>();

	/** Enum multi-select filters - using String values to avoid conversion issues */
	private Map<String, List<String>> enumFilters = new HashMap<>();

	public Map<String, LocalDate[]> getDateFilters() {
		// Initialize arrays for all DATE properties if not exists
		getDynamicColumns().stream()
			.filter(c -> c.getFilterType() == FilterType.DATE)
			.forEach(c -> dateFilters.computeIfAbsent(c.getProperty(), k -> new LocalDate[2]));
		return dateFilters;
	}

	public Map<String, LocalDateTime[]> getDateTimeFilters() {
		// Initialize arrays for all DATETIME properties if not exists
		getDynamicColumns().stream()
			.filter(c -> c.getFilterType() == FilterType.DATETIME)
			.forEach(c -> dateTimeFilters.computeIfAbsent(c.getProperty(), k -> new LocalDateTime[2]));
		return dateTimeFilters;
	}

	public Map<String, List<String>> getEnumFilters() {
		// Initialize empty lists for all ENUM properties if not exists
		getDynamicColumns().stream()
			.filter(c -> c.getFilterType() == FilterType.ENUM)
			.forEach(c -> enumFilters.computeIfAbsent(c.getProperty(), k -> new ArrayList<>()));
		return enumFilters;
	}


	public List<ColumnDef> getDynamicColumns() {
		if (dynamicColumns.isEmpty()) {
			// Populate columns based on Customer entity declared fields
			Field[] fields = Customer.class.getDeclaredFields();
			for (Field f : fields) {
				// skip static fields like serialVersionUID
				if (Modifier.isStatic(f.getModifiers()))
					continue;
				String prop = f.getName();
				if ("serialVersionUID".equals(prop))
					continue;
				// build a human readable header from camelCase property name
				String header = toHeader(prop);
				FilterType filterType = detectFilterType(f);
				dynamicColumns.add(new ColumnDef(header, prop, filterType, f.getType()));
			}
			// default: preselect first 5 columns
			if (selectedColumnProperties.isEmpty()) {
				for (int i = 0; i < dynamicColumns.size() && i < 5; i++) {
					selectedColumnProperties.add(dynamicColumns.get(i).getProperty());
				}
			}
		}
		return dynamicColumns;
	}

	private FilterType detectFilterType(Field field) {
		Class<?> type = field.getType();
		
		if (type.isEnum()) {
			return FilterType.ENUM;
		} else if (type == boolean.class || type == Boolean.class) {
			return FilterType.BOOLEAN;
		} else if (type == LocalDate.class) {
			return FilterType.DATE;
		} else if (type == LocalDateTime.class) {
			return FilterType.DATETIME;
		} else if (type == int.class || type == Integer.class || 
				   type == long.class || type == Long.class ||
				   type == double.class || type == Double.class ||
				   type == float.class || type == Float.class) {
			return FilterType.NUMBER;
		} else if (type == String.class) {
			return FilterType.TEXT;
		} else {
			return FilterType.OBJECT;
		}
	}

	/**
	 * Get all values for enum filters - returns as String values
	 */
	public List<SelectItem> getEnumOptions(String property) {
		ColumnDef col = dynamicColumns.stream()
			.filter(c -> c.getProperty().equals(property))
			.findFirst()
			.orElse(null);
		
		if (col != null && col.getFilterType() == FilterType.ENUM) {
			Class<?> enumClass = col.getFieldType();
			if (enumClass.isEnum()) {
				return Arrays.stream(enumClass.getEnumConstants())
					.map(e -> new SelectItem(e.toString(), e.toString())) // Use string value
					.collect(Collectors.toList());
			}
		}
		return new ArrayList<>();
	}

	private String toHeader(String prop) {
		// split camelCase and underscores into words and capitalize
		StringBuilder sb = new StringBuilder();
		char[] cs = prop.toCharArray();
		boolean prevUpper = false;
		for (int i = 0; i < cs.length; i++) {
			char c = cs[i];
			if (i == 0) {
				sb.append(Character.toUpperCase(c));
				prevUpper = Character.isUpperCase(c);
				continue;
			}
			if (c == '_' || (Character.isUpperCase(c) && !prevUpper)) {
				sb.append(' ');
			}
			sb.append(c);
			prevUpper = Character.isUpperCase(c);
		}
		// special-case some property names
		return sb.toString().replace("CustomerRank", "Rank");
	}

	/**
	 * Returns SelectItem list for the column selector dropdown. Each item has
	 * label=header, value=property name.
	 */
	public List<SelectItem> getAllColumnOptions() {
		getDynamicColumns(); // ensure populated
		List<SelectItem> items = new ArrayList<>();
		for (ColumnDef c : dynamicColumns) {
			items.add(new SelectItem(c.getProperty(), c.getHeader()));
		}
		return items;
	}

	/**
	 * Called when user changes column selection in the dropdown.
	 */
	public void onColumnSelectionChange() {
		// selectedColumnProperties is already updated by JSF binding
		// table will re-render via AJAX update
	}

	public List<String> getSelectedColumnProperties() {
		return selectedColumnProperties;
	}

	public void setSelectedColumnProperties(List<String> selectedColumnProperties) {
		this.selectedColumnProperties = selectedColumnProperties;
	}

	/** Helper used by UI to decide if a column property should be rendered. */
	public boolean isColumnVisible(String property) {
		return selectedColumnProperties.contains(property);
	}

	/**
	 * Returns the appropriate filter match mode based on filter type
	 */
	public String getFilterMatchMode(FilterType filterType) {
		switch (filterType) {
			case TEXT:
			case OBJECT:
				return "contains";
			case NUMBER:
				return "equals";
			case BOOLEAN:
				return "equals";
			case ENUM:
				return "custom"; // Use custom filter for enum multi-select
			case DATE:
			case DATETIME:
				return "between"; // 'between' mode for date ranges
			default:
				return "contains";
		}
	}

	/**
	 * Custom filter function for enum multi-select
	 * This is called by PrimeFaces for each row when filtering
	 */
	public boolean filterEnum(Object value, Object filter, java.util.Locale locale) {
		// If no filter is set, show all rows
		if (filter == null) {
			return true;
		}

		// Handle List filter (from selectCheckboxMenu)
		if (filter instanceof List) {
			@SuppressWarnings("unchecked")
			List<String> selectedValues = (List<String>) filter;
			
			// If nothing selected, show all
			if (selectedValues == null || selectedValues.isEmpty()) {
				return true;
			}

			// If the cell value is null, don't show it
			if (value == null) {
				return false;
			}

			// Check if the enum value matches any selected value (as string)
			String valueStr = value.toString();
			return selectedValues.contains(valueStr);
		}

		// Default: show the row
		return true;
	}

	/**
	 * Custom filter function for number ranges (supports syntax: 5, 5.., ..10, 5..10)
	 */
	public boolean filterNumber(Object value, Object filter, java.util.Locale locale) {
		if (filter == null || filter.toString().trim().isEmpty()) {
			return true;
		}

		if (value == null) {
			return false;
		}

		try {
			double numValue = ((Number) value).doubleValue();
			String filterStr = filter.toString().trim();

			// Exact match: "5"
			if (!filterStr.contains("..")) {
				return numValue == Double.parseDouble(filterStr);
			}

			// Range: "5..10" or "5.." or "..10"
			String[] parts = filterStr.split("\\.\\.");
			if (parts.length == 2) {
				// "5..10"
				double min = parts[0].isEmpty() ? Double.MIN_VALUE : Double.parseDouble(parts[0].trim());
				double max = parts[1].isEmpty() ? Double.MAX_VALUE : Double.parseDouble(parts[1].trim());
				return numValue >= min && numValue <= max;
			} else if (filterStr.startsWith("..")) {
				// "..10"
				double max = Double.parseDouble(filterStr.substring(2).trim());
				return numValue <= max;
			} else if (filterStr.endsWith("..")) {
				// "5.."
				double min = Double.parseDouble(filterStr.substring(0, filterStr.length() - 2).trim());
				return numValue >= min;
			}
		} catch (Exception e) {
			// Invalid filter format
		}

		return true;
	}

	/**
	 * Custom filter function for date ranges
	 */
	public boolean filterDate(Object value, Object filter, java.util.Locale locale) {
		if (filter == null) {
			return true;
		}

		if (value == null) {
			return false;
		}

		if (filter instanceof LocalDate[]) {
			LocalDate[] range = (LocalDate[]) filter;
			LocalDate dateValue = (LocalDate) value;

			LocalDate from = range[0];
			LocalDate to = range[1];

			if (from == null && to == null) {
				return true;
			}

			if (from != null && to != null) {
				return !dateValue.isBefore(from) && !dateValue.isAfter(to);
			} else if (from != null) {
				return !dateValue.isBefore(from);
			} else {
				return !dateValue.isAfter(to);
			}
		}

		return true;
	}

	/**
	 * Custom filter function for datetime ranges
	 */
	public boolean filterDateTime(Object value, Object filter, java.util.Locale locale) {
		if (filter == null) {
			return true;
		}

		if (value == null) {
			return false;
		}

		if (filter instanceof LocalDateTime[]) {
			LocalDateTime[] range = (LocalDateTime[]) filter;
			LocalDateTime dateTimeValue = (LocalDateTime) value;

			LocalDateTime from = range[0];
			LocalDateTime to = range[1];

			if (from == null && to == null) {
				return true;
			}

			if (from != null && to != null) {
				return !dateTimeValue.isBefore(from) && !dateTimeValue.isAfter(to);
			} else if (from != null) {
				return !dateTimeValue.isBefore(from);
			} else {
				return !dateTimeValue.isAfter(to);
			}
		}

		return true;
	}

	/**
	 * Legacy method kept for backward compatibility if remoteCommand is still
	 * called.
	 */
	public void updateVisibleColumns() {
		// no-op: now using p:ajax from selectCheckboxMenu
	}
}
