package com.axonivy.market.extendedtable.demo.beans;

import java.util.List;

import com.axonivy.market.extendedtable.demo.entities.Customer;

public class NoneLazyBean extends GenericDemoBean {

	private List<Customer> items;

	public void init() {
		items = customerService.findAll();
	}

	public List<Customer> getItems() {
		return items;
	}

}