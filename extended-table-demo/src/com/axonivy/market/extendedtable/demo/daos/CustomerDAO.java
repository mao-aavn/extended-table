package com.axonivy.market.extendedtable.demo.daos;

import com.axonivy.market.extendedtable.demo.entities.Customer;
import com.axonivy.market.extendedtable.demo.entities.Customer_;
import com.axonivy.utils.persistence.dao.AuditableIdDAO;
import com.axonivy.utils.persistence.dao.CriteriaQueryContext;
import com.axonivy.utils.persistence.dao.CriteriaQueryGenericContext;

import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomerDAO extends AuditableIdDAO<Customer_, Customer> implements BaseDAO {

	@Override
	protected Class<Customer> getType() {
		return Customer.class;
	}

	private List<Predicate> buildFilterPredicates(Map<String, FilterMeta> filterBy,
			From<?, ?> root, CriteriaBuilder cb) {
		List<Predicate> predicates = new ArrayList<>();
		if (filterBy != null) {
			for (FilterMeta filter : filterBy.values()) {
				Object filterValue = filter.getFilterValue();
				if (filterValue == null || filterValue.toString().trim().isEmpty()) {
					continue;
				}
				String field = filter.getField();
				Path<?> path = root;
				// Support nested attributes (e.g., "group.name" or "country.name")
				String[] parts = field.split("\\.");
				for (int i = 0; i < parts.length; i++) {
					if (i == parts.length - 1) {
						path = path.get(parts[i]);
					} else {
						// Always join for parent attributes in the path
						if (path instanceof From) {
							path = ((From<?, ?>) path).join(parts[i], JoinType.LEFT);
						} else {
							throw new IllegalArgumentException("Cannot join on non-From path for: " + parts[i]);
						}
					}
				}
				Class<?> type = path.getJavaType();

				if (Enum.class.isAssignableFrom(type)) {
					try {
						@SuppressWarnings("unchecked")
						Enum<?> enumValue = (filterValue instanceof Enum) ? (Enum<?>) filterValue
								: Enum.valueOf((Class<Enum>) type, filterValue.toString());
						predicates.add(cb.equal(path, enumValue));
					} catch (Exception e) {
						// ignore invalid enum filter
					}
				} else if (Number.class.isAssignableFrom(type)) {
					try {
						Number number = (filterValue instanceof Number) ? (Number) filterValue
								: Double.valueOf(filterValue.toString());
						predicates.add(cb.equal(path, number));
					} catch (NumberFormatException e) {
						// ignore invalid number filter
					}
				} else if (java.util.Date.class.isAssignableFrom(type)
						|| java.time.temporal.Temporal.class.isAssignableFrom(type)) {
					if (filterValue instanceof Map) {
						Map<?, ?> range = (Map<?, ?>) filterValue;
						Object from = range.get("from");
						Object to = range.get("to");
						if (from != null) {
							predicates.add(cb.greaterThanOrEqualTo(path.as(Comparable.class), (Comparable) from));
						}
						if (to != null) {
							predicates.add(cb.lessThanOrEqualTo(path.as(Comparable.class), (Comparable) to));
						}
					} else {
						predicates.add(cb.equal(path, filterValue));
					}
				} else if (type == String.class) {
					predicates.add(cb.like(cb.lower(path.as(String.class)),
							"%" + filterValue.toString().toLowerCase() + "%"));
				} else {
					predicates.add(cb.equal(path, filterValue));
				}
			}
		}
		return predicates;
	}

	public List<Customer> find(int offset, int pageSize, Map<String, SortMeta> sortBy,
			Map<String, FilterMeta> filterBy) {
		try (CriteriaQueryContext<Customer> ctx = initializeQuery()) {
			List<Predicate> predicates = buildFilterPredicates(filterBy, ctx.r, ctx.c);
			if (!predicates.isEmpty()) {
				ctx.q.where(ctx.c.and(predicates.toArray(new Predicate[0])));
			}

			// Sorting
			List<Order> orders = new ArrayList<>();
			if (sortBy != null && !sortBy.isEmpty()) {
				for (SortMeta sortMeta : sortBy.values()) {
					String field = sortMeta.getField();
					if (sortMeta.getOrder().isAscending()) {
						orders.add(ctx.c.asc(ctx.r.get(field)));
					} else {
						orders.add(ctx.c.desc(ctx.r.get(field)));
					}
				}
			}
			if (!orders.isEmpty()) {
				ctx.q.orderBy(orders);
			}

			ctx.getQuerySettings().withFirstResult(offset).withMaxResults(pageSize);

			return findByCriteria(ctx);
		}
	}

	public int count(Map<String, FilterMeta> filterBy) {
		try (CriteriaQueryGenericContext<Customer,Long> ctx = initializeQuery(getType(), Long.class)) {
			List<Predicate> predicates = buildFilterPredicates(filterBy, ctx.r, ctx.c);
			if (!predicates.isEmpty()) {
				ctx.q.where(ctx.c.and(predicates.toArray(new Predicate[0])));
			}

			ctx.q.select(ctx.c.count(ctx.r));
			Long count = findByCriteria(ctx).stream().findFirst().orElse(0L);
			return count != null ? count.intValue() : 0;
		}
	}
}
