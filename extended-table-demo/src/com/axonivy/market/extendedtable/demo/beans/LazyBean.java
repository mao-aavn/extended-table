package com.axonivy.market.extendedtable.demo.beans;

import org.primefaces.model.LazyDataModel;

import com.axonivy.market.extendedtable.demo.entities.Customer;
import com.axonivy.market.extendedtable.demo.model.LazyCustomerDataModel;

public class LazyBean extends GenericDemoBean {

	private LazyDataModel<Customer> lazyModel;

	@Override
	protected void loadItems() {
		lazyModel = new LazyCustomerDataModel();
	}

	public LazyDataModel<Customer> getLazyModel() {
		return lazyModel;
	}

}