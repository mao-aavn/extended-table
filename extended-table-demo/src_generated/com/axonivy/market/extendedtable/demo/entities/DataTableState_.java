package com.axonivy.market.extendedtable.demo.entities;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(DataTableState.class)
public abstract class DataTableState_ extends com.axonivy.utils.persistence.beans.AuditableIdEntity_ {
	public static volatile SingularAttribute<DataTableState, String> stateKey;
	public static volatile SingularAttribute<DataTableState, String> stateValue;

	public static final String STATE_KEY = "stateKey";
	public static final String STATE_VALUE = "stateValue";
}
