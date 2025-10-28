package com.axonivy.market.extendedtable.demo.beans;

import com.axonivy.market.extendedtable.demo.entities.Customer;

public class SingleRowSelectionBean extends GenericDemoBean {

	private Customer selectedCustomer;

	public Customer getSelectedCustomer() {
		return selectedCustomer;
	}

	public void setSelectedCustomer(Customer selectedCustomer) {
		this.selectedCustomer = selectedCustomer;
	}

}
