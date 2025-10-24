package com.axonivy.market.extendedtable.demo.beans;

import java.util.List;

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

}
