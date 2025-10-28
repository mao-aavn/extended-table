package com.axonivy.market.extendedtable.demo.entities;

import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(CustomerGroup.class)
public abstract class CustomerGroup_ extends com.axonivy.utils.persistence.beans.AuditableIdEntity_ {

	public static volatile SingularAttribute<CustomerGroup, String> name;
	public static volatile SingularAttribute<CustomerGroup, String> leader;
	public static volatile SingularAttribute<CustomerGroup, String> description;
	public static volatile ListAttribute<CustomerGroup, Customer> customers;

	public static final String NAME = "name";
	public static final String LEADER = "leader";
	public static final String DESCRIPTION = "description";
	public static final String CUSTOMERS = "customers";

}
