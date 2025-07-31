package com.axonivy.market.extendedtable.demo.daos;

import com.axonivy.market.extendedtable.demo.entities.Customer;
import com.axonivy.market.extendedtable.demo.entities.Customer_;
import com.axonivy.utils.persistence.dao.AuditableIdDAO;

public class CustomerDAO extends AuditableIdDAO<Customer_, Customer> implements BaseDAO {

	@Override
	protected Class<Customer> getType() {
		return Customer.class;
	}

}
