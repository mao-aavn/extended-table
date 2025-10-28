package com.axonivy.market.extendedtable.demo.entities;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(Country.class)
public abstract class Country_ extends com.axonivy.utils.persistence.beans.AuditableIdEntity_ {

	public static volatile SingularAttribute<Country, String> name;
	public static volatile SingularAttribute<Country, String> code;
	public static volatile SingularAttribute<Country, Boolean> rtl;

	public static final String NAME = "name";
	public static final String CODE = "code";
	public static final String RTL = "rtl";

}
