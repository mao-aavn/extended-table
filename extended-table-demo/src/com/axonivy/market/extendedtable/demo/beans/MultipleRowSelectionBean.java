package com.axonivy.market.extendedtable.demo.beans;

import java.util.ArrayList;
import java.util.List;

import com.axonivy.market.extendedtable.demo.entities.Customer;

public class MultipleRowSelectionBean extends GenericDemoBean {
	private List<Customer> selectedCustomers = new ArrayList<>();

	public List<Customer> getSelectedCustomers() {
		return selectedCustomers;
	}

	public void setSelectedCustomers(List<Customer> selectedCustomers) {
		this.selectedCustomers = selectedCustomers;
	}

}
