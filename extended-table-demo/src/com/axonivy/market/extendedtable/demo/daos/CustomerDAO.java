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

	private String mapFieldName(String field) {
		// Handle field name aliases for backward compatibility
		if ("rank".equals(field)) {
			return "customerRank";
		}
		// Handle nested fields like "rank.something" -> "customerRank.something"
		if (field.startsWith("rank.")) {
			return field.replaceFirst("^rank\\.", "customerRank.");
		}
		return field;
	}

	private Path<?> resolvePath(String field, From<?, ?> root, Map<String, Join<?, ?>> joinCache) {
		// Map field name to actual entity field
		field = mapFieldName(field);
		
		Path<?> path = root;
		// Support nested attributes (e.g., "group.name" or "country.name")
		String[] parts = field.split("\\.");
		for (int i = 0; i < parts.length; i++) {
			if (i == parts.length - 1) {
				path = path.get(parts[i]);
			} else {
				// Always join for parent attributes in the path, reusing cached joins
				if (path instanceof From) {
					String joinKey = String.join(".", java.util.Arrays.copyOfRange(parts, 0, i + 1));
					Join<?, ?> join = joinCache.get(joinKey);
					if (join == null) {
						join = ((From<?, ?>) path).join(parts[i], JoinType.LEFT);
						joinCache.put(joinKey, join);
					}
					path = join;
				} else {
					throw new IllegalArgumentException("Cannot join on non-From path for: " + parts[i]);
				}
			}
		}
		return path;
	}

	private List<Predicate> buildFilterPredicates(Map<String, FilterMeta> filterBy,
			From<?, ?> root, CriteriaBuilder cb, Map<String, Join<?, ?>> joinCache) {
		List<Predicate> predicates = new ArrayList<>();
		if (filterBy == null) {
			return predicates;
		}

		for (FilterMeta filter : filterBy.values()) {
			Object filterValue = filter.getFilterValue();
			if (isEmptyFilter(filterValue)) {
				continue;
			}

			String field = filter.getField();
			Path<?> path = resolvePath(field, root, joinCache);
			Class<?> type = path.getJavaType();

			Predicate predicate = buildPredicateForType(path, type, filterValue, cb);
			if (predicate != null) {
				predicates.add(predicate);
			}
		}
		return predicates;
	}

	private boolean isEmptyFilter(Object filterValue) {
		return filterValue == null || filterValue.toString().trim().isEmpty();
	}

	private Predicate buildPredicateForType(Path<?> path, Class<?> type, Object filterValue, CriteriaBuilder cb) {
		if (type == Boolean.class || type == boolean.class) {
			return buildBooleanPredicate(path, filterValue, cb);
		} else if (Enum.class.isAssignableFrom(type)) {
			return buildEnumPredicate(path, type, filterValue, cb);
		} else if (isNumberType(type)) {
			return buildNumberPredicate(path, filterValue, cb);
		} else if (isDateType(type)) {
			return buildDatePredicate(path, filterValue, cb);
		} else if (type == String.class) {
			return buildStringPredicate(path, filterValue, cb);
		} else {
			return cb.equal(path, filterValue);
		}
	}

	private boolean isNumberType(Class<?> type) {
		return Number.class.isAssignableFrom(type) || type == int.class || type == long.class
				|| type == double.class || type == float.class || type == short.class || type == byte.class;
	}

	private boolean isDateType(Class<?> type) {
		return java.util.Date.class.isAssignableFrom(type)
				|| java.time.temporal.Temporal.class.isAssignableFrom(type);
	}

	private Predicate buildBooleanPredicate(Path<?> path, Object filterValue, CriteriaBuilder cb) {
		try {
			Boolean boolValue = (filterValue instanceof Boolean) ? (Boolean) filterValue
					: Boolean.valueOf(filterValue.toString());
			return cb.equal(path, boolValue);
		} catch (Exception e) {
			return null; // ignore invalid boolean filter
		}
	}

	private Predicate buildEnumPredicate(Path<?> path, Class<?> type, Object filterValue, CriteriaBuilder cb) {
		try {
			if (filterValue instanceof java.util.Collection) {
				return buildEnumCollectionPredicate(path, type, (java.util.Collection<?>) filterValue, cb);
			} else if (filterValue instanceof Enum) {
				return cb.equal(path, filterValue);
			} else {
				return buildEnumFromString(path, type, filterValue.toString().trim(), cb);
			}
		} catch (Exception e) {
			return null; // ignore invalid enum filter
		}
	}

	private Predicate buildEnumCollectionPredicate(Path<?> path, Class<?> type, java.util.Collection<?> enumValues,
			CriteriaBuilder cb) {
		if (enumValues.isEmpty()) {
			return null;
		}

		List<Predicate> enumPredicates = new ArrayList<>();
		for (Object enumItem : enumValues) {
			if (enumItem instanceof Enum) {
				enumPredicates.add(cb.equal(path, enumItem));
			} else if (enumItem != null) {
				String enumStr = enumItem.toString().trim();
				if (!enumStr.isEmpty()) {
					@SuppressWarnings({ "unchecked", "rawtypes" })
					Enum<?> enumValue = Enum.valueOf((Class<Enum>) type, enumStr);
					enumPredicates.add(cb.equal(path, enumValue));
				}
			}
		}
		return enumPredicates.isEmpty() ? null : cb.or(enumPredicates.toArray(new Predicate[0]));
	}

	private Predicate buildEnumFromString(Path<?> path, Class<?> type, String enumStr, CriteriaBuilder cb) {
		if (enumStr.isEmpty()) {
			return null;
		}
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Enum<?> enumValue = Enum.valueOf((Class<Enum>) type, enumStr);
		return cb.equal(path, enumValue);
	}

	private Predicate buildNumberPredicate(Path<?> path, Object filterValue, CriteriaBuilder cb) {
		String filterStr = filterValue.toString();
		if (filterStr.contains("..")) {
			return buildNumberRangeFromString(path, filterStr, cb);
		} else if (filterValue instanceof Map) {
			return buildRangeFromMap(path, (Map<?, ?>) filterValue, cb);
		} else {
			return buildExactNumberPredicate(path, filterValue, cb);
		}
	}

	private Predicate buildNumberRangeFromString(Path<?> path, String filterStr, CriteriaBuilder cb) {
		// Use limit -1 to preserve trailing empty strings
		String[] parts = filterStr.split("\\.\\.", -1);
		if (parts.length != 2) {
			return null;
		}
		
		List<Predicate> predicates = new ArrayList<>();
		Class<?> type = path.getJavaType();
		
		try {
			// Handle "from.." (e.g., "5.." means >= 5)
			if (!parts[0].trim().isEmpty()) {
				Number from = parseNumber(parts[0].trim(), type);
				@SuppressWarnings("unchecked")
				Expression<Number> pathExpr = (Expression<Number>) path;
				predicates.add(cb.ge(pathExpr, from));
			}
			
			// Handle "..to" (e.g., "..10" means <= 10)
			if (!parts[1].trim().isEmpty()) {
				Number to = parseNumber(parts[1].trim(), type);
				@SuppressWarnings("unchecked")
				Expression<Number> pathExpr = (Expression<Number>) path;
				predicates.add(cb.le(pathExpr, to));
			}
			
			return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
		} catch (NumberFormatException e) {
			return null; // ignore invalid range
		}
	}
	
	private Number parseNumber(String value, Class<?> type) {
		if (type == Integer.class || type == int.class) {
			return Integer.valueOf(value);
		} else if (type == Long.class || type == long.class) {
			return Long.valueOf(value);
		} else if (type == Double.class || type == double.class) {
			return Double.valueOf(value);
		} else if (type == Float.class || type == float.class) {
			return Float.valueOf(value);
		} else if (type == Short.class || type == short.class) {
			return Short.valueOf(value);
		} else if (type == Byte.class || type == byte.class) {
			return Byte.valueOf(value);
		} else {
			// Default to Double for other Number types
			return Double.valueOf(value);
		}
	}

	private Predicate buildExactNumberPredicate(Path<?> path, Object filterValue, CriteriaBuilder cb) {
		try {
			Number number = (filterValue instanceof Number) ? (Number) filterValue
					: Double.valueOf(filterValue.toString());
			return cb.equal(path, number);
		} catch (NumberFormatException e) {
			return null; // ignore invalid number filter
		}
	}

	private Predicate buildDatePredicate(Path<?> path, Object filterValue, CriteriaBuilder cb) {
		if (filterValue instanceof Map) {
			return buildRangeFromMap(path, (Map<?, ?>) filterValue, cb);
		} else if (filterValue instanceof java.util.List) {
			return buildDateRangeFromList(path, (java.util.List<?>) filterValue, cb);
		} else {
			return cb.equal(path, filterValue);
		}
	}

	@SuppressWarnings("unchecked")
	private Predicate buildRangeFromMap(Path<?> path, Map<?, ?> range, CriteriaBuilder cb) {
		Object from = range.get("from");
		Object to = range.get("to");
		
		List<Predicate> predicates = new ArrayList<>();
		if (from != null) {
			@SuppressWarnings("rawtypes")
			Expression<Comparable> pathExpr = (Expression<Comparable>) path;
			predicates.add(cb.greaterThanOrEqualTo(pathExpr, (Comparable) from));
		}
		if (to != null) {
			Expression<Comparable> pathExpr = (Expression<Comparable>) path;
			predicates.add(cb.lessThanOrEqualTo(pathExpr, (Comparable) to));
		}
		return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
	}

	private Predicate buildDateRangeFromList(Path<?> path, java.util.List<?> dateList, CriteriaBuilder cb) {
		if (dateList == null || dateList.isEmpty()) {
			return null;
		}

		// Check if the path is LocalDateTime type
		boolean isLocalDateTime = path.getJavaType().equals(java.time.LocalDateTime.class);

		// Multiple selection: Build IN query for selectionMode="multiple"
		CriteriaBuilder.In<Object> inClause = cb.in(path);
		for (Object date : dateList) {
			if (date != null) {
				// Convert LocalDate to LocalDateTime if needed
				if (isLocalDateTime && date instanceof java.time.LocalDate) {
					// For IN clause, use start of day
					inClause.value(((java.time.LocalDate) date).atStartOfDay());
				} else {
					inClause.value(date);
				}
			}
		}
		return inClause;
	}

	private Predicate buildStringPredicate(Path<?> path, Object filterValue, CriteriaBuilder cb) {
		return cb.like(cb.lower(path.as(String.class)),
				"%" + filterValue.toString().toLowerCase() + "%");
	}

	private List<Order> buildSortOrders(Map<String, SortMeta> sortBy, From<?, ?> root, CriteriaBuilder cb,
			Map<String, Join<?, ?>> joinCache) {
		List<Order> orders = new ArrayList<>();
		if (sortBy == null || sortBy.isEmpty()) {
			return orders;
		}

		for (SortMeta sortMeta : sortBy.values()) {
			String field = sortMeta.getField();
			Path<?> sortPath = resolvePath(field, root, joinCache);
			if (sortMeta.getOrder().isAscending()) {
				orders.add(cb.asc(sortPath));
			} else {
				orders.add(cb.desc(sortPath));
			}
		}
		return orders;
	}

	public List<Customer> find(int offset, int pageSize, Map<String, SortMeta> sortBy,
			Map<String, FilterMeta> filterBy) {
		try (CriteriaQueryContext<Customer> ctx = initializeQuery()) {
			Map<String, Join<?, ?>> joinCache = new java.util.HashMap<>();

			List<Predicate> predicates = buildFilterPredicates(filterBy, ctx.r, ctx.c, joinCache);
			if (!predicates.isEmpty()) {
				ctx.q.where(ctx.c.and(predicates.toArray(new Predicate[0])));
			}

			List<Order> orders = buildSortOrders(sortBy, ctx.r, ctx.c, joinCache);
			if (!orders.isEmpty()) {
				ctx.q.orderBy(orders);
			}

			ctx.getQuerySettings().withFirstResult(offset).withMaxResults(pageSize);

			return findByCriteria(ctx);
		}
	}

	public int count(Map<String, FilterMeta> filterBy) {
		try (CriteriaQueryGenericContext<Customer,Long> ctx = initializeQuery(getType(), Long.class)) {
			Map<String, Join<?, ?>> joinCache = new java.util.HashMap<>();
			
			List<Predicate> predicates = buildFilterPredicates(filterBy, ctx.r, ctx.c, joinCache);
			if (!predicates.isEmpty()) {
				ctx.q.where(ctx.c.and(predicates.toArray(new Predicate[0])));
			}

			ctx.q.select(ctx.c.count(ctx.r));
			Long count = findByCriteria(ctx).stream().findFirst().orElse(0L);
			return count != null ? count.intValue() : 0;
		}
	}
}
