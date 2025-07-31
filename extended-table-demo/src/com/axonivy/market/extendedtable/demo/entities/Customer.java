package com.axonivy.market.extendedtable.demo.entities;

import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.Entity;

import com.axonivy.utils.persistence.beans.AuditableIdEntity;

@Entity
public class Customer extends AuditableIdEntity {

	private static final long serialVersionUID = 1L;

	private String name;
	private String company;
	private Country country;
	private LocalDate date;
	private CustomerStatus status;
	private int activity;
	private Representative representative;

	public Customer() {
	}

	public Customer(int id, String name, String company, Country country, LocalDate date, CustomerStatus status,
			int activity, Representative representative) {
		this.name = name;
		this.company = company;
		this.country = country;
		this.date = date;
		this.status = status;
		this.activity = activity;
		this.representative = representative;
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

	public Representative getRepresentative() {
		return representative;
	}

	public void setRepresentative(Representative representative) {
		this.representative = representative;
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
				&& Objects.equals(date, customer.date) && status == customer.status
				&& Objects.equals(representative, customer.representative);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, company, country, date, status, activity, representative);
	}
}