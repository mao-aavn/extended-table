package com.axonivy.market.extendedtable.demo.beans;

import java.util.ArrayList;
import java.util.List;

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