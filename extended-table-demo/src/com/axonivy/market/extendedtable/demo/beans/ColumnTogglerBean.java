package com.axonivy.market.extendedtable.demo.beans;

import java.util.List;

import com.axonivy.market.extendedtable.demo.entities.Customer;
import com.axonivy.market.extendedtable.demo.entities.CustomerStatus;

public class ColumnTogglerBean extends GenericDemoBean {
	private List<Customer> items;

	// Properties for rank range filtering
	private Integer rankFrom;
	private Integer rankTo;
	private List<CustomerStatus> selectedStatuses;

	public void init() {
		items = customerService.findAll();
	}

	public List<Customer> getItems() {
		return items;
	}

	public Integer getRankFrom() {
		return rankFrom;
	}

	public void setRankFrom(Integer rankFrom) {
		this.rankFrom = rankFrom;
	}

	public Integer getRankTo() {
		return rankTo;
	}

	public void setRankTo(Integer rankTo) {
		this.rankTo = rankTo;
	}

	public List<CustomerStatus> getSelectedStatuses() {
		return selectedStatuses;
	}

	public void setSelectedStatuses(List<CustomerStatus> selectedStatuses) {
		this.selectedStatuses = selectedStatuses;
	}

}
