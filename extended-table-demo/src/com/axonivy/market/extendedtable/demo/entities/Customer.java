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

	private int customerRank;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "group_id")
	private CustomerGroup group;

	private boolean isNew = false;

	// Builder pattern
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String name;
		private String company;
		private Country country;
		private LocalDate date;
		private CustomerStatus status;
		private int customerRank;
		private CustomerGroup group;
		private boolean isNew = false;

		public Builder name(String name) {
			this.name = name;
			return this;
		}
		public Builder company(String company) {
			this.company = company;
			return this;
		}
		public Builder country(Country country) {
			this.country = country;
			return this;
		}
		public Builder date(LocalDate date) {
			this.date = date;
			return this;
		}
		public Builder status(CustomerStatus status) {
			this.status = status;
			return this;
		}

		// keep backward compatibility with previous API used in services/UI
		public Builder rank(int rank) {
			this.customerRank = rank;
			return this;
		}

		public Builder customerRank(int customerRank) {
			this.customerRank = customerRank;
			return this;
		}

		public Builder group(CustomerGroup group) {
			this.group = group;
			return this;
		}
		public Builder isNew(boolean isNew) {
			this.isNew = isNew;
			return this;
		}
		public Customer build() {
			Customer c = new Customer();
			c.setName(name);
			c.setCompany(company);
			c.setCountry(country);
			c.setDate(date);
			c.setStatus(status);
			c.setCustomerRank(customerRank);
			c.setGroup(group);
			c.setNew(isNew);
			return c;
		}
	}

	public Customer() {
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

	public int getCustomerRank() {
		return customerRank;
	}

	public void setCustomerRank(int customerRank) {
		this.customerRank = customerRank;
	}

	// convenience accessors for UI bindings to "rank"
	public int getRank() {
		return customerRank;
	}

	public void setRank(int rank) {
		this.customerRank = rank;
	}



	public CustomerGroup getGroup() {
		return group;
	}

	public void setGroup(CustomerGroup group) {
		this.group = group;
	}

	public boolean isNew() {
		return isNew;
	}

	public void setNew(boolean isNew) {
		this.isNew = isNew;
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
	return id == customer.id && customerRank == customer.customerRank && Objects.equals(name, customer.name)
				&& Objects.equals(company, customer.company) && Objects.equals(country, customer.country)
				&& Objects.equals(date, customer.date) && status == customer.status;
	}

	@Override
	public int hashCode() {
	return Objects.hash(id, name, company, country, date, status, customerRank);
	}
}
