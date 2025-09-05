package com.axonivy.market.extendedtable.demo.daos;

import javax.persistence.criteria.Predicate;

import com.axonivy.market.extendedtable.demo.entities.DataTableState;
import com.axonivy.market.extendedtable.demo.entities.DataTableState_;
import com.axonivy.utils.persistence.dao.AuditableIdDAO;
import com.axonivy.utils.persistence.dao.CriteriaQueryContext;

public class DataTableStateDAO extends AuditableIdDAO<DataTableState_, DataTableState> implements BaseDAO {

	@Override
	protected Class<DataTableState> getType() {
		return DataTableState.class;
	}

	public DataTableState findByKey(String key) {
		try (CriteriaQueryContext<DataTableState> ctx = initializeQuery()) {
			Predicate byKey = ctx.c.equal(ctx.r.get(DataTableState_.STATE_KEY), key);
			ctx.q.where(byKey);
			return findByCriteria(ctx).stream().findFirst().orElse(null);
		}
	}

}
