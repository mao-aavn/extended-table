package com.axonivy.market.extendedtable.demo.beans;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.axonivy.market.extendedtable.demo.entities.Customer;


@ViewScoped
@ManagedBean(name = "columnTogglerShowcaseBean")
public class ColumnTogglerShowcaseBean extends GenericShowcaseBean {
	private List<Customer> items;

	@PostConstruct
	public void init() {
		items = customerService.findAll();
	}

	public List<Customer> getItems() {
		return items;
	}

}