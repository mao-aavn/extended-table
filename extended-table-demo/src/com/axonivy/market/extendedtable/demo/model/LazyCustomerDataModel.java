package com.axonivy.market.extendedtable.demo.model;

import java.util.List;
import java.util.Map;

import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

import com.axonivy.market.extendedtable.demo.daos.CustomerDAO;
import com.axonivy.market.extendedtable.demo.entities.Customer;

public class LazyCustomerDataModel extends LazyDataModel<Customer> {

	private static final long serialVersionUID = 1L;

	private CustomerDAO customerDAO = new CustomerDAO();

	public LazyCustomerDataModel() {
	}

	@Override
	public Customer getRowData(String rowKey) {
		return customerDAO.findById(rowKey);
	}

	@Override
	public String getRowKey(Customer customer) {
		return String.valueOf(customer.getId());
	}

	@Override
	public int count(Map<String, FilterMeta> filterBy) {
		return customerDAO.count(filterBy);
		
	}

	@Override
	public List<Customer> load(int offset, int pageSize, Map<String, SortMeta> sortBy,
			Map<String, FilterMeta> filterBy) {
		List<Customer> result = customerDAO.find(offset, pageSize, sortBy, filterBy);
		this.setRowCount(customerDAO.count(filterBy));
		return result;
	}

}
