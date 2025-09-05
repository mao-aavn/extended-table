package com.axonivy.market.extendedtable.demo.daos;

/**
 * Define the persistence unit name to use in DAOs implementing this one.
 */
public interface BaseDAO extends com.axonivy.utils.persistence.dao.BaseDAO {
	@Override
	default public String getPersistenceUnitName() {
		return "extended_table_demo";
	}
}
