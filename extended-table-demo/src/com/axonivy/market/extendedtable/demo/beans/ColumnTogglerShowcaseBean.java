package com.axonivy.market.extendedtable.demo.beans;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.axonivy.market.extendedtable.demo.entities.Customer;


@ViewScoped
@ManagedBean(name = "columnTogglerShowcaseBean")
public class ColumnTogglerShowcaseBean extends GenericShowcaseBean {
	private List<Customer> items;

	// Properties for rank range filtering
	private Integer rankFrom;
	private Integer rankTo;

	@PostConstruct
	public void init() {
		items = customerService.findAll();
	}

	public List<Customer> getItems() {
		return items;
	}

	public Integer getRankFrom() {
		return rankFrom;
	}

	public void setRankFrom(Integer rankFrom) {
		this.rankFrom = rankFrom;
	}

	public Integer getRankTo() {
		return rankTo;
	}

	public void setRankTo(Integer rankTo) {
		this.rankTo = rankTo;
	}

	// Custom filter function for rank with syntax: "x..y", "..y", "x..", or single value "x"
	public boolean filterRank(Object value, Object filter, Locale locale) {
		if (value == null) {
			return false;
		}

		Integer rank = null;
		if (value instanceof Number) {
			rank = ((Number) value).intValue();
		} else if (value instanceof String) {
			try {
				rank = Integer.parseInt((String) value);
			} catch (NumberFormatException e) {
				return false;
			}
		}

		if (filter == null) {
			return true; // no filter
		}

		String text = Objects.toString(filter, "").trim();
		if (text.isEmpty()) {
			return true;
		}

		// Normalize unicode dots and spaces
		text = text.replaceAll("\u2026", "..").replaceAll("\s+", "");

		try {
			if (text.contains("..")) {
				String[] parts = text.split("\\.\\.", -1);
				String left = parts.length > 0 ? parts[0] : "";
				String right = parts.length > 1 ? parts[1] : "";

				Integer from = left.isEmpty() ? null : Integer.valueOf(left);
				Integer to = right.isEmpty() ? null : Integer.valueOf(right);

				if (from != null && rank < from) {
					return false;
				}
				if (to != null && rank > to) {
					return false;
				}
				return true;
			} else {
				// exact
				return rank.equals(Integer.valueOf(text));
			}
		} catch (NumberFormatException ex) {
			return false;
		}
	}

}