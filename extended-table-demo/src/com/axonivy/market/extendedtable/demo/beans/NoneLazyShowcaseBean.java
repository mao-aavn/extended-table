package com.axonivy.market.extendedtable.demo.beans;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;

import com.axonivy.market.extendedtable.demo.entities.Customer;

@ManagedBean(name = "noneLazyShowcaseBean")
public class NoneLazyShowcaseBean extends GenericShowcaseBean {

	private List<Customer> items;

	@PostConstruct
	public void init() {
		items = customerService.findAll();
	}

	public List<Customer> getItems() {
		return items;
	}

}