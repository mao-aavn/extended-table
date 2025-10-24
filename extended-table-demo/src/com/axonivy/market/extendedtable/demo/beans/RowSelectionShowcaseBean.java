package com.axonivy.market.extendedtable.demo.beans;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.primefaces.event.SelectEvent;

import com.axonivy.market.extendedtable.demo.entities.Customer;
import com.axonivy.market.extendedtable.demo.entities.CustomerStatus;

@ViewScoped
@ManagedBean(name = "rowSelectionShowcaseBean")
public class RowSelectionShowcaseBean extends GenericShowcaseBean {

	private List<Customer> items;
	private Customer selectedCustomer;
	private List<Customer> selectedCustomers = new ArrayList<>();

	private List<Customer> filteredItems;
	private List<CustomerStatus> selectedStatuses;
	private List<LocalDate> dateRangeFilter; // holds 0..2 dates from the date picker

	@PostConstruct
	public void init() {
		customerService.initCustomersIfNotExisting(500);
		items = customerService.findAll();
		filteredItems = new ArrayList<>(items);
	}

	public List<Customer> getItems() {
		return items;
	}

	public List<Customer> getFilteredItems() {
		return filteredItems;
	}

	public List<LocalDate> getDateRangeFilter() {
		return dateRangeFilter;
	}

	public void setDateRangeFilter(List<LocalDate> dateRangeFilter) {
		this.dateRangeFilter = dateRangeFilter;
	}

	public Customer getSelectedCustomer() {
		return selectedCustomer;
	}

	public void setSelectedCustomer(Customer selectedCustomer) {
		this.selectedCustomer = selectedCustomer;
	}

	public List<Customer> getSelectedCustomers() {
		return selectedCustomers;
	}

	public void setSelectedCustomers(List<Customer> selectedCustomers) {
		this.selectedCustomers = selectedCustomers;
	}

	public List<CustomerStatus> getSelectedStatuses() {
		return selectedStatuses;
	}

	public void setSelectedStatuses(List<CustomerStatus> selectedStatuses) {
		this.selectedStatuses = selectedStatuses;
	}

	public void setFilteredItems(List<Customer> filteredItems) {
		this.filteredItems = filteredItems;
	}

	public void onRowSelect(SelectEvent<Customer> event) {
		Object source = event.getSource();
		Object selection = ((org.primefaces.component.datatable.DataTable) source).getSelection();

		StringBuilder names = new StringBuilder();
		if (selection instanceof List) {
			List<?> list = (List<?>) selection;
			if (!list.isEmpty()) {
				for (Object obj : list) {
					if (obj instanceof Customer) {
						if (names.length() > 0) {
							names.append(", ");
						}
						names.append(((Customer) obj).getName());
					}
				}
			}
		} else if (selection instanceof Customer) {
			names.append(((Customer) selection).getName());
		}

		FacesMessage msg = new FacesMessage("Row Selection", "Selected: " + names);
		FacesContext.getCurrentInstance().addMessage(null, msg);
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

		// PrimeFaces datePicker with selectionMode=range usually posts a List of 2 dates
		if (filter instanceof java.util.List) {
			@SuppressWarnings("unchecked")
			java.util.List<Object> range = (java.util.List<Object>) filter;
			LocalDate from = null;
			LocalDate to = null;
			if (range.size() > 0 && range.get(0) instanceof java.util.Date) {
				from = ((java.util.Date) range.get(0)).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
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
