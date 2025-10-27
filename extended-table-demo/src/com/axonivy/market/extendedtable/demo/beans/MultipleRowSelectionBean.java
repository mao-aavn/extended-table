package com.axonivy.market.extendedtable.demo.beans;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.primefaces.event.SelectEvent;

import com.axonivy.market.extendedtable.demo.entities.Customer;
import com.axonivy.market.extendedtable.demo.entities.CustomerStatus;

public class MultipleRowSelectionBean extends GenericDemoBean {

	private List<Customer> items;
	private Customer selectedCustomer;
	private List<Customer> selectedCustomers = new ArrayList<>();

	private List<Customer> filteredItems;
	private List<CustomerStatus> selectedStatuses;
	private List<LocalDate> dateRangeFilter; // holds 0..2 dates from the date picker

	public void init() {
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

}
