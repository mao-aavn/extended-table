package com.axonivy.market.extendedtable.demo.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.axonivy.utils.persistence.beans.AuditableIdEntity;

@Entity
public class Customer extends AuditableIdEntity {

	private static final long serialVersionUID = 1L;

	private String name;
	private String company;

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "country_id")
	private Country country;

	private LocalDateTime dateTime;

	private LocalDate date;
	
	private LocalDate date1;

	@Enumerated(EnumType.STRING)
	private CustomerStatus status;

	private int customerRank;
	
	private double income;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "group_id")
	private CustomerGroup group;

	private boolean hasRepresentative = false;

	// Builder pattern
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String name;
		private String company;
		private Country country;
		private LocalDate date;
		private LocalDate date1;
		private LocalDateTime dateTime;
		private CustomerStatus status;
		private int customerRank;
		private double income;
		private CustomerGroup group;
		private boolean hasRepresentative = false;

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
		
		public Builder date1(LocalDate date) {
			this.date1 = date;
			return this;
		}
		
		public Builder dateTime(LocalDateTime dateTime) {
			this.dateTime = dateTime;
			return this;
		}

		public Builder status(CustomerStatus status) {
			this.status = status;
			return this;
		}

		public Builder rank(int rank) {
			this.customerRank = rank;
			return this;
		}
		
		public Builder income(double income) {
			this.income = income;
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

		public Builder hasRepresentative(boolean hasRepresentative) {
			this.hasRepresentative = hasRepresentative;
			return this;
		}

		public Customer build() {
			Customer c = new Customer();
			c.setName(name);
			c.setCompany(company);
			c.setCountry(country);
			c.setDate(date);
			c.setDate1(date1);
			c.setDateTime(dateTime);
			c.setStatus(status);
			c.setCustomerRank(customerRank);
			c.setIncome(income);
			c.setGroup(group);
			c.setHasRepresentative(hasRepresentative);
			return c;
		}
	}

	public Customer() {
	}

	public LocalDate getDate1() {
		return date1;
	}

	public void setDate1(LocalDate date1) {
		this.date1 = date1;
	}

	public double getIncome() {
		return income;
	}

	public void setIncome(double income) {
		this.income = income;
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

	public LocalDateTime getDateTime() {
		return dateTime;
	}

	public void setDateTime(LocalDateTime dateTime) {
		this.dateTime = dateTime;
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

	public boolean isHasRepresentative() {
		return hasRepresentative;
	}

	public void setHasRepresentative(boolean hasRepresentative) {
		this.hasRepresentative = hasRepresentative;
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
		return id == customer.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "Customer [name=" + name + ", company=" + company + ", country=" + country + ", dateTime=" + dateTime
				+ ", date=" + date + ", date1=" + date1 + ", status=" + status + ", customerRank=" + customerRank
				+ ", income=" + income + ", group=" + group + ", hasRepresentative=" + hasRepresentative + "]";
	}
	
	
}
