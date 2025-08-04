package com.axonivy.market.extendedtable.demo.entities;

import java.util.Objects;

import javax.persistence.Entity;

import com.axonivy.utils.persistence.beans.AuditableIdEntity;

@Entity
public class Country extends AuditableIdEntity {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private String code;
	private boolean rtl;

	public Country() {
	}

	public Country(String name, String code) {
		this.name = name;
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public boolean isRtl() {
		return rtl;
	}

	public void setRtl(boolean rtl) {
		this.rtl = rtl;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Country country = (Country) o;
		return id == country.id && Objects.equals(name, country.name) && Objects.equals(code, country.code);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, code);
	}

	@Override
	public String toString() {
		return name;
	}

}