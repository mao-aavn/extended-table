package com.axonivy.market.extendedtable.demo.beans;

import static java.time.format.DateTimeFormatter.ofPattern;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.primefaces.component.datatable.DataTable;
import org.primefaces.event.SelectEvent;

import com.axonivy.market.extendedtable.demo.entities.Country;
import com.axonivy.market.extendedtable.demo.entities.Customer;
import com.axonivy.market.extendedtable.demo.entities.CustomerStatus;
import com.axonivy.market.extendedtable.demo.service.CustomerService;

import ch.ivyteam.ivy.environment.Ivy;

public abstract class GenericDemoBean {
	private final DateTimeFormatter dateTimeFormatter = ofPattern("dd.MM.yyyy MM:ss");
	private final DateTimeFormatter dateFormatter = ofPattern("dd.MM.yyyy");

	protected CustomerService customerService = new CustomerService();

	protected List<Customer> items;
	protected List<Customer> filteredItems;
	protected List<Customer> selectedItems;

	protected List<CustomerStatus> selectedStatuses;
	protected final CustomerStatus[] customerStatus = CustomerStatus.values();
	protected List<Country> countries = null;

	protected LocalDate dateFilter; 
	protected List<LocalDate> datesFilter; // for date range and multiple modes only
	protected List<LocalDate> dateTimesFilter; // for date range and multiple modes only
	protected Integer rankFrom;
	protected Integer rankTo;

	/**
	 * Template method that defines the initialization flow. Subclasses cannot
	 * override this method due to final modifier. To customize initialization,
	 * override the loadItems() hook method instead.
	 */
	public final void init() {
		loadItems();
		countries = customerService.getCountries();
	}

	/**
	 * Hook method for subclasses to load their specific data. By default, loads all
	 * customers from the service. Override this method to customize data loading
	 * behavior.
	 */
	protected void loadItems() {
		items = customerService.findAll();
	}

	/**
	 * Handles row selection events from the data table. Displays a FacesMessage
	 * with the names of selected customers. Supports both single and multiple
	 * selection modes.
	 * 
	 * @param event The SelectEvent containing the selection information
	 */
	public void onRowSelect(SelectEvent<Customer> event) {
		Object source = event.getSource();
		if (!(source instanceof DataTable)) {
			return;
		}

		Object selection = ((DataTable) source).getSelection();
		String selectedNames = getSelectedCustomerNames(selection);

		if (!selectedNames.isEmpty()) {
			FacesMessage msg = new FacesMessage("Row Selection", "Selected: " + selectedNames);
			FacesContext.getCurrentInstance().addMessage(null, msg);
		}
	}

	/**
	 * Extracts customer names from the selection object. Handles both single
	 * Customer objects and Lists of Customers.
	 * 
	 * @param selection The selection object from the data table
	 * @return A comma-separated string of customer names
	 */
	private String getSelectedCustomerNames(Object selection) {
		if (selection == null) {
			return "";
		}

		if (selection instanceof List) {
			List<?> list = (List<?>) selection;
			return list.stream().filter(obj -> obj instanceof Customer).map(obj -> ((Customer) obj).getName())
					.collect(Collectors.joining(", "));
		} else if (selection instanceof Customer) {
			return ((Customer) selection).getName();
		}

		return "";
	}

	// Custom date filter supporting selectionMode="range" from DatePicker
	// Also supports single date selection and multiple date selection
	// The filter parameter can be: LocalDate (single), List<LocalDate> (range/multiple), String, or java.util.Date
	public boolean filterDate(Object value, Object filter, Locale locale) {
		LocalDate date = null;

		// Convert value to LocalDate
		if (value instanceof LocalDate) {
			date = (LocalDate) value;
		} else if (value instanceof LocalDateTime) {
			date = ((LocalDateTime) value).toLocalDate();
		} else {
			return false;
		}

		if (filter == null) {
			return true;
		}
		
		// Handle String filter (format: "dd.MM.yyyy,dd.MM.yyyy")
		if (filter instanceof String) {
			String filterStr = ((String) filter).trim();
			if (filterStr.isEmpty()) {
				return true;
			}
			
			try {
				String[] parts = filterStr.split(",");
				LocalDate from = null;
				LocalDate to = null;
				
				if (parts.length > 0 && !parts[0].trim().isEmpty()) {
					from = LocalDate.parse(parts[0].trim(), dateFormatter);
				}
				if (parts.length > 1 && !parts[1].trim().isEmpty()) {
					to = LocalDate.parse(parts[1].trim(), dateFormatter);
				}
				
				if (from != null && date.isBefore(from)) {
					return false;
				}
				if (to != null && date.isAfter(to)) {
					return false;
				}
				return true;
			} catch (Exception e) {
			return true; // If parsing fails, show all
		}
	}

	// Handle List: can be range (2 dates) or multiple selection (n dates)
	if (filter instanceof java.util.List) {
		@SuppressWarnings("unchecked")
		java.util.List<Object> filterList = (java.util.List<Object>) filter;
		
		if (filterList.isEmpty()) {
			return true;
		}
		
		// If exactly 2 dates, treat as range (from..to)
		if (filterList.size() == 2) {
			LocalDate from = null;
			LocalDate to = null;
			
			if (filterList.get(0) instanceof java.util.Date) {
				from = ((java.util.Date) filterList.get(0)).toInstant()
					.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
			} else if (filterList.get(0) instanceof LocalDate) {
				from = (LocalDate) filterList.get(0);
			}
			
			if (filterList.get(1) instanceof java.util.Date) {
				to = ((java.util.Date) filterList.get(1)).toInstant()
					.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
			} else if (filterList.get(1) instanceof LocalDate) {
				to = (LocalDate) filterList.get(1);
			}

			if (from != null && date.isBefore(from)) {
				return false;
			}
			if (to != null && date.isAfter(to)) {
				return false;
			}
			return true;
		}
		
		// Multiple selection: check if date matches any in the list
		for (Object filterItem : filterList) {
			LocalDate filterDate = null;
			
			if (filterItem instanceof java.util.Date) {
				filterDate = ((java.util.Date) filterItem).toInstant()
					.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
			} else if (filterItem instanceof LocalDate) {
				filterDate = (LocalDate) filterItem;
			}
			
			if (filterDate != null && date.equals(filterDate)) {
				return true;
			}
		}
		return false;
	}		// Single value compare: equals
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
	public boolean filterNumber(Object value, Object filter, Locale locale) {
		if (value == null) {
			return false;
		}

		Number numValue = null;
		if (value instanceof Number) {
			numValue = (Number) value;
		} else if (value instanceof String) {
			try {
				// Try to parse as double to handle both int and double
				numValue = Double.parseDouble((String) value);
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

				Double from = left.isEmpty() ? null : Double.valueOf(left);
				Double to = right.isEmpty() ? null : Double.valueOf(right);

				double numVal = numValue.doubleValue();
				
				if (from != null && numVal < from) {
					return false;
				}
				if (to != null && numVal > to) {
					return false;
				}
				return true;
			} else {
				// exact match - compare as double to handle both int and double
				return Math.abs(numValue.doubleValue() - Double.parseDouble(text)) < 0.0001;
			}
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	/**
	 * Custom filter function for country names that handles multiple selection.
	 * Filters by country name (String).
	 * 
	 * @param value  The actual country name from the data row
	 * @param filter The filter value(s) from the selectCheckboxMenu (List of
	 *               String)
	 * @param locale The locale (not used but required by PrimeFaces filter
	 *               signature)
	 * @return true if the value matches the filter criteria, false otherwise
	 */
	public boolean filterCountry(Object value, Object filter, Locale locale) {
		// Null value never matches
		if (value == null) {
			return false;
		}

		// No filter means show all
		if (filter == null) {
			return true;
		}

		String countryName = value.toString();

		// Handle List of filter values (multi-select)
		if (filter instanceof List) {
			List<?> filterList = (List<?>) filter;

			// Empty list means no filter applied
			if (filterList.isEmpty()) {
				return true;
			}

			// Check if countryName matches any item in the filter list
			for (Object filterItem : filterList) {
				if (filterItem != null && countryName.equals(filterItem.toString())) {
					return true;
				}
			}
			return false;
		}

		// Handle single filter value
		return countryName.equals(filter.toString());
	}

	public String formatDateTime(LocalDateTime dt) {
		if (dt == null) {
			return "";
		}

		return dt.format(dateTimeFormatter);
	}

	public String formatDate(LocalDate dt) {
		if (dt == null) {
			return "";
		}

		return dt.format(dateFormatter);
	}

	// getters/setters
	
	public List<CustomerStatus> getSelectedStatuses() {
		return selectedStatuses;
	}

	public List<LocalDate> getDatesFilter() {
		return datesFilter;
	}

	public void setDatesFilter(List<LocalDate> datesFilter) {
		this.datesFilter = datesFilter;
	}

	public List<LocalDate> getDateTimesFilter() {
		return dateTimesFilter;
	}

	public void setDateTimesFilter(List<LocalDate> dateTimesFilter) {
		this.dateTimesFilter = dateTimesFilter;
	}

	public void setSelectedStatuses(List<CustomerStatus> selectedStatuses) {
		this.selectedStatuses = selectedStatuses;
	}

	public Integer getRankFrom() {
		return rankFrom;
	}

	public void setRankFrom(Integer rankFrom) {
		this.rankFrom = rankFrom;
	}

	public Integer getRankTo() {
		return rankTo;
	}

	public void setRankTo(Integer rankTo) {
		this.rankTo = rankTo;
	}

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

	public List<Customer> getItems() {
		return items;
	}

	public void setItems(List<Customer> items) {
		this.items = items;
	}

	public List<Country> getCountries() {
		return countries;
	}

	public void setCountries(List<Country> countries) {
		this.countries = countries;
	}

	public CustomerStatus[] getCustomerStatus() {
		return customerStatus;
	}

	public LocalDate getDateFilter() {
		return dateFilter;
	}

	public void setDateFilter(LocalDate dateFilter) {
		this.dateFilter = dateFilter;
	}

}
