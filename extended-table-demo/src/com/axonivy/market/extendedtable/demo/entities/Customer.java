package com.axonivy.market.extendedtable.demo.entities;

import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.JoinColumn;

import com.axonivy.utils.persistence.beans.AuditableIdEntity;

@Entity
public class Customer extends AuditableIdEntity {

	private static final long serialVersionUID = 1L;

	private String name;
	private String company;

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "country_id")
	private Country country;

	private LocalDate date;
	
	@Enumerated(EnumType.STRING)
	private CustomerStatus status;
	
	private int activity;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "group_id")
	private CustomerGroup group;

	public Customer() {
	}

	public Customer(String name, String company, Country country, LocalDate date, CustomerStatus status,
			int activity) {
		this.name = name;
		this.company = company;
		this.country = country;
		this.date = date;
		this.status = status;
		this.activity = activity;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public Country getCountry() {
		return country;
	}

	public void setCountry(Country country) {
		this.country = country;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public CustomerStatus getStatus() {
		return status;
	}

	public void setStatus(CustomerStatus status) {
		this.status = status;
	}

	public int getActivity() {
		return activity;
	}

	public void setActivity(int activity) {
		this.activity = activity;
	}

	public CustomerGroup getGroup() {
		return group;
	}

	public void setGroup(CustomerGroup group) {
		this.group = group;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Customer customer = (Customer) o;
		return id == customer.id && activity == customer.activity && Objects.equals(name, customer.name)
				&& Objects.equals(company, customer.company) && Objects.equals(country, customer.country)
				&& Objects.equals(date, customer.date) && status == customer.status;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, company, country, date, status, activity);
	}
}
