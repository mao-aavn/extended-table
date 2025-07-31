package com.axonivy.market.extendedtable.demo.model;

import java.util.Comparator;

import org.primefaces.model.SortOrder;

import com.axonivy.market.extendedtable.demo.entities.Customer;
import com.axonivy.market.extendedtable.utils.ObjectUtil;

public class LazySorter implements Comparator<Customer> {

	private String sortField;
	private SortOrder sortOrder;

	public LazySorter(String sortField, SortOrder sortOrder) {
		this.sortField = sortField;
		this.sortOrder = sortOrder;
	}

	@Override
	public int compare(Customer customer1, Customer customer2) {
		try {
			Object value1 = ObjectUtil.getPropertyValueViaReflection(customer1, sortField);
			Object value2 = ObjectUtil.getPropertyValueViaReflection(customer2, sortField);

			if (value1 == null || value2 == null) {
				return -1;
			}

			int value = ((Comparable) value1).compareTo(value2);

			return SortOrder.ASCENDING.equals(sortOrder) ? value : -1 * value;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}