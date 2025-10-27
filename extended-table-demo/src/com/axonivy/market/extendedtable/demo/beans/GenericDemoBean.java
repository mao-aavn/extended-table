package com.axonivy.market.extendedtable.demo.beans;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.axonivy.market.extendedtable.demo.entities.Customer;
import com.axonivy.market.extendedtable.demo.entities.CustomerStatus;
import com.axonivy.market.extendedtable.demo.service.CustomerService;

import ch.ivyteam.ivy.environment.Ivy;

public abstract class GenericDemoBean {

	protected CustomerService customerService = new CustomerService();

	public CustomerStatus[] getCustomerStatus() {
		return CustomerStatus.values();
	}

	private List<Customer> filteredItems;
	private List<Customer> selectedItems;

	public List<Customer> getFilteredItems() {
		return filteredItems;
	}

	public void setFilteredItems(List<Customer> filteredItems) {
		this.filteredItems = filteredItems;
	}

	public List<Customer> getSelectedItems() {
		return selectedItems;
	}

	public void setSelectedItems(List<Customer> selectedItems) {
		this.selectedItems = selectedItems;
	}

	// Custom date filter supporting selectionMode="range" from DatePicker
	public boolean filterDate(Object value, Object filter, Locale locale) {
		if (!(value instanceof LocalDate)) {
			return false;
		}
		LocalDate date = (LocalDate) value;

		if (filter == null) {
			return true;
		}

		// PrimeFaces datePicker with selectionMode=range usually posts a List of 2
		// dates
		if (filter instanceof java.util.List) {
			@SuppressWarnings("unchecked")
			java.util.List<Object> range = (java.util.List<Object>) filter;
			LocalDate from = null;
			LocalDate to = null;
			if (range.size() > 0 && range.get(0) instanceof java.util.Date) {
				from = ((java.util.Date) range.get(0)).toInstant().atZone(java.time.ZoneId.systemDefault())
						.toLocalDate();
			} else if (range.size() > 0 && range.get(0) instanceof LocalDate) {
				from = (LocalDate) range.get(0);
			}
			if (range.size() > 1 && range.get(1) instanceof java.util.Date) {
				to = ((java.util.Date) range.get(1)).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
			} else if (range.size() > 1 && range.get(1) instanceof LocalDate) {
				to = (LocalDate) range.get(1);
			}

			if (from != null && date.isBefore(from)) {
				return false;
			}
			if (to != null && date.isAfter(to)) {
				return false;
			}
			return true;
		}

		// Single value compare: equals
		if (filter instanceof java.util.Date) {
			LocalDate f = ((java.util.Date) filter).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
			return date.equals(f);
		}
		if (filter instanceof LocalDate) {
			return date.equals((LocalDate) filter);
		}

		return true;
	}

	/**
	 * Generic filter function for enum values that handles both direct enum
	 * comparison and string-based comparison after serialization/deserialization.
	 * This filter supports both single values and lists of values (for multi-select
	 * filters).
	 * 
	 * Usage in XHTML:
	 * 
	 * <pre>
	 * &lt;p:column filterBy="#{item.status}" filterMatchMode="custom" 
	 *           filterFunction="#{data.bean.filterEnum}"&gt;
	 * </pre>
	 * 
	 * @param value  The actual value from the data row (should be an Enum)
	 * @param filter The filter value(s) from the UI component (can be Enum, String,
	 *               or List)
	 * @param locale The locale (not used but required by PrimeFaces filter
	 *               signature)
	 * @return true if the value matches the filter criteria, false otherwise
	 */
	public boolean filterEnum(Object value, Object filter, java.util.Locale locale) {
		// Null value never matches
		if (value == null) {
			return false;
		}

		// No filter means show all
		if (filter == null) {
			return true;
		}

		// Ensure value is an enum
		if (!(value instanceof Enum<?>)) {
			Ivy.log().warn("filterEnum called with non-enum value: {0}", value.getClass().getName());
			return false;
		}

		Enum<?> enumValue = (Enum<?>) value;

		// Handle List of filter values (multi-select)
		if (filter instanceof List) {
			List<?> filterList = (List<?>) filter;

			// Empty list means no filter applied
			if (filterList.isEmpty()) {
				return true;
			}

			// Check if enumValue matches any item in the filter list
			for (Object filterItem : filterList) {
				if (matchesEnum(enumValue, filterItem)) {
					return true;
				}
			}
			return false;
		}

		// Handle single filter value
		return matchesEnum(enumValue, filter);
	}

	/**
	 * Helper method to compare an enum value with a filter item. Handles both
	 * direct enum comparison and string-based comparison.
	 * 
	 * @param enumValue  The enum value to compare
	 * @param filterItem The filter item (can be Enum or String)
	 * @return true if they match, false otherwise
	 */
	private boolean matchesEnum(Enum<?> enumValue, Object filterItem) {
		if (filterItem == null) {
			return false;
		}

		// Direct enum comparison
		if (filterItem instanceof Enum<?>) {
			return enumValue.equals(filterItem);
		}

		// String comparison (after serialization/deserialization)
		if (filterItem instanceof String) {
			String filterStr = (String) filterItem;
			return enumValue.name().equals(filterStr) || enumValue.toString().equals(filterStr);
		}

		return false;
	}

	// Custom filter function for rank with syntax: "x..y", "..y", "x..", or single
	// value "x"
	public boolean filterRank(Object value, Object filter, Locale locale) {
		if (value == null) {
			return false;
		}

		Integer rank = null;
		if (value instanceof Number) {
			rank = ((Number) value).intValue();
		} else if (value instanceof String) {
			try {
				rank = Integer.parseInt((String) value);
			} catch (NumberFormatException e) {
				return false;
			}
		}

		if (filter == null) {
			return true; // no filter
		}

		String text = Objects.toString(filter, "").trim();
		if (text.isEmpty()) {
			return true;
		}

		// Normalize unicode dots and spaces
		text = text.replaceAll("\u2026", "..").replaceAll("\s+", "");

		try {
			if (text.contains("..")) {
				String[] parts = text.split("\\.\\.", -1);
				String left = parts.length > 0 ? parts[0] : "";
				String right = parts.length > 1 ? parts[1] : "";

				Integer from = left.isEmpty() ? null : Integer.valueOf(left);
				Integer to = right.isEmpty() ? null : Integer.valueOf(right);

				if (from != null && rank < from) {
					return false;
				}
				if (to != null && rank > to) {
					return false;
				}
				return true;
			} else {
				// exact
				return rank.equals(Integer.valueOf(text));
			}
		} catch (NumberFormatException ex) {
			return false;
		}
	}

}
