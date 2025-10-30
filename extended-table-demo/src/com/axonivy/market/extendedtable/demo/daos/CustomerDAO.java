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
		if (filterBy != null) {
			for (FilterMeta filter : filterBy.values()) {
				Object filterValue = filter.getFilterValue();
				if (filterValue == null || filterValue.toString().trim().isEmpty()) {
					continue;
				}
				String field = filter.getField();
				Path<?> path = resolvePath(field, root, joinCache);
				Class<?> type = path.getJavaType();

				if (type == Boolean.class || type == boolean.class) {
					try {
						Boolean boolValue = (filterValue instanceof Boolean) ? (Boolean) filterValue
								: Boolean.valueOf(filterValue.toString());
						predicates.add(cb.equal(path, boolValue));
					} catch (Exception e) {
						// ignore invalid boolean filter
					}
				} else if (Enum.class.isAssignableFrom(type)) {
					try {
						ch.ivyteam.ivy.environment.Ivy.log().info("DEBUG: Enum filter - field: " + field + ", type: " + type + ", filterValue: " + filterValue + ", filterValue class: " + filterValue.getClass());
						
						if (filterValue instanceof java.util.Collection) {
							// Handle multi-select enum filter
							java.util.Collection<?> enumValues = (java.util.Collection<?>) filterValue;
							ch.ivyteam.ivy.environment.Ivy.log().info("DEBUG: filterValue is a Collection with " + enumValues.size() + " items");
							if (!enumValues.isEmpty()) {
								List<Predicate> enumPredicates = new ArrayList<>();
								for (Object enumItem : enumValues) {
									if (enumItem instanceof Enum) {
										enumPredicates.add(cb.equal(path, enumItem));
									} else if (enumItem != null) {
										String enumStr = enumItem.toString().trim();
										if (!enumStr.isEmpty()) {
											@SuppressWarnings("unchecked")
											Enum<?> enumValue = Enum.valueOf((Class<Enum>) type, enumStr);
											enumPredicates.add(cb.equal(path, enumValue));
										}
									}
								}
								if (!enumPredicates.isEmpty()) {
									predicates.add(cb.or(enumPredicates.toArray(new Predicate[0])));
									ch.ivyteam.ivy.environment.Ivy.log().info("DEBUG: Added OR predicate with " + enumPredicates.size() + " enum values");
								}
							}
						} else if (filterValue instanceof Enum) {
							ch.ivyteam.ivy.environment.Ivy.log().info("DEBUG: filterValue is already an Enum, using directly");
							predicates.add(cb.equal(path, filterValue));
						} else {
							String enumStr = filterValue.toString().trim();
							ch.ivyteam.ivy.environment.Ivy.log().info("DEBUG: filterValue as string (trimmed): '" + enumStr + "', isEmpty: " + enumStr.isEmpty());
							if (!enumStr.isEmpty()) {
								@SuppressWarnings("unchecked")
								Enum<?> enumValue = Enum.valueOf((Class<Enum>) type, enumStr);
								ch.ivyteam.ivy.environment.Ivy.log().info("DEBUG: Parsed enum value: " + enumValue);
								predicates.add(cb.equal(path, enumValue));
							}
						}
					} catch (IllegalArgumentException e) {
						// ignore invalid enum filter - value doesn't exist in enum
						ch.ivyteam.ivy.environment.Ivy.log().info("DEBUG: IllegalArgumentException parsing enum: " + e.getMessage());
					} catch (Exception e) {
						// ignore other invalid enum filter
						ch.ivyteam.ivy.environment.Ivy.log().info("DEBUG: Exception parsing enum: " + e.getClass() + " - " + e.getMessage());
					}
				} else if (Number.class.isAssignableFrom(type) || type == int.class || type == long.class 
						|| type == double.class || type == float.class || type == short.class || type == byte.class) {
					// Check if it's a range filter (format: "min..max")
					String filterStr = filterValue.toString();
					if (filterStr.contains("..")) {
						// Parse range filter
						String[] parts = filterStr.split("\\.\\.");
						if (parts.length == 2) {
							try {
								Double from = Double.valueOf(parts[0].trim());
								Double to = Double.valueOf(parts[1].trim());
								predicates.add(cb.greaterThanOrEqualTo(path.as(Double.class), from));
								predicates.add(cb.lessThanOrEqualTo(path.as(Double.class), to));
							} catch (NumberFormatException e) {
								// ignore invalid range
							}
						}
					} else if (filterValue instanceof Map) {
						// Handle range filter as Map (alternative format)
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
						try {
							Number number = (filterValue instanceof Number) ? (Number) filterValue
									: Double.valueOf(filterValue.toString());
							predicates.add(cb.equal(path, number));
						} catch (NumberFormatException e) {
							// ignore invalid number filter
						}
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
			Map<String, Join<?, ?>> joinCache = new java.util.HashMap<>();
			
			List<Predicate> predicates = buildFilterPredicates(filterBy, ctx.r, ctx.c, joinCache);
			if (!predicates.isEmpty()) {
				ctx.q.where(ctx.c.and(predicates.toArray(new Predicate[0])));
			}

			// Sorting
			List<Order> orders = new ArrayList<>();
			if (sortBy != null && !sortBy.isEmpty()) {
				for (SortMeta sortMeta : sortBy.values()) {
					String field = sortMeta.getField();
					Path<?> sortPath = resolvePath(field, ctx.r, joinCache);
					if (sortMeta.getOrder().isAscending()) {
						orders.add(ctx.c.asc(sortPath));
					} else {
						orders.add(ctx.c.desc(sortPath));
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
