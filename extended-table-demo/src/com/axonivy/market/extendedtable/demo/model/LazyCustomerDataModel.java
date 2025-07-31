package com.axonivy.market.extendedtable.demo.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.faces.context.FacesContext;

import org.apache.commons.collections4.ComparatorUtils;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import org.primefaces.model.filter.FilterConstraint;
import org.primefaces.util.LocaleUtils;

import com.axonivy.market.extendedtable.demo.entities.Customer;
import com.axonivy.market.extendedtable.utils.ObjectUtil;


public class LazyCustomerDataModel extends LazyDataModel<Customer> {

	private static final long serialVersionUID = 1L;

	private List<Customer> datasource = new ArrayList<>();

	public LazyCustomerDataModel(List<Customer> datasource) {
		this.datasource = datasource;
	}
	
	public LazyCustomerDataModel() {
	}

	@Override
	public Customer getRowData(String rowKey) {
		for (Customer customer : datasource) {
			if (customer.getId().equals(rowKey)) {
				return customer;
			}
		}

		return null;
	}

	@Override
	public String getRowKey(Customer customer) {
		return String.valueOf(customer.getId());
	}

	@Override
	public int count(Map<String, FilterMeta> filterBy) {
		return (int) datasource.stream().filter(o -> filter(FacesContext.getCurrentInstance(), filterBy.values(), o))
				.count();
	}

	private boolean filter(FacesContext context, Collection<FilterMeta> filterBy, Object o) {
		boolean matching = true;

		for (FilterMeta filter : filterBy) {
			FilterConstraint constraint = filter.getConstraint();
			Object filterValue = filter.getFilterValue();

			Object columnValue = String.valueOf(ObjectUtil.getPropertyValueViaReflection(o, filter.getField()));
			matching = constraint.isMatching(context, columnValue, filterValue, LocaleUtils.getCurrentLocale());

			if (!matching) {
				break;
			}
		}

		return matching;
	}

	@Override
	public List<Customer> load(int offset, int pageSize, Map<String, SortMeta> sortBy,
			Map<String, FilterMeta> filterBy) {
		// apply offset & filters
		List<Customer> customers = datasource.stream()
				.filter(o -> filter(FacesContext.getCurrentInstance(), filterBy.values(), o))
				.collect(Collectors.toList());

		// sort
		if (!sortBy.isEmpty()) {
			List<Comparator<Customer>> comparators = sortBy.values().stream()
					.map(o -> new LazySorter(o.getField(), o.getOrder())).collect(Collectors.toList());
			Comparator<Customer> cp = ComparatorUtils.chainedComparator(comparators); // from apache
			customers.sort(cp);
		}

		return customers.subList(offset, Math.min(offset + pageSize, customers.size()));
	}

}