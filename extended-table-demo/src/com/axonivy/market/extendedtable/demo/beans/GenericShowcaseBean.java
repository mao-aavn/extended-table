package com.axonivy.market.extendedtable.demo.beans;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import com.axonivy.market.extendedtable.demo.entities.Customer;
import com.axonivy.market.extendedtable.demo.entities.CustomerStatus;
import com.axonivy.market.extendedtable.demo.service.CustomerService;

public abstract class GenericShowcaseBean {

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

}
