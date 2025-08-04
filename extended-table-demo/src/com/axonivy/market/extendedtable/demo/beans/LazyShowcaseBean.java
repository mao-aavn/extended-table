package com.axonivy.market.extendedtable.demo.beans;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.LazyDataModel;

import com.axonivy.market.extendedtable.demo.entities.Customer;
import com.axonivy.market.extendedtable.demo.model.LazyCustomerDataModel;

import ch.ivyteam.ivy.environment.Ivy;

@ViewScoped
@ManagedBean(name = "lazyShowcaseBean")
public class LazyShowcaseBean extends GenericShowcaseBean {

	private LazyDataModel<Customer> lazyModel;

	@PostConstruct
	public void init() {
		lazyModel = new LazyCustomerDataModel();
	}

	public LazyDataModel<Customer> getLazyModel() {
		return lazyModel;
	}

	public void onPage() {
		Ivy.log().info("onPage");
	}

}