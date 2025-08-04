package com.axonivy.market.extendedtable.demo.entities;


import java.time.LocalDate;
import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(Customer.class)
public abstract class Customer_ extends com.axonivy.utils.persistence.beans.AuditableIdEntity_ {

	public static volatile SingularAttribute<Customer, LocalDate> date;
	public static volatile SingularAttribute<Customer, Country> country;
	public static volatile SingularAttribute<Customer, Integer> activity;
	public static volatile SingularAttribute<Customer, String> name;
	public static volatile SingularAttribute<Customer, String> company;
	public static volatile SingularAttribute<Customer, CustomerStatus> status;
	public static volatile SingularAttribute<Customer, CustomerGroup> group;

	public static final String DATE = "date";
	public static final String COUNTRY = "country";
	public static final String ACTIVITY = "activity";
	public static final String NAME = "name";
	public static final String COMPANY = "company";
	public static final String STATUS = "status";
	public static final String GROUP = "group";
}
