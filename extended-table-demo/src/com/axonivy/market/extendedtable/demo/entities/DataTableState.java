package com.axonivy.market.extendedtable.demo.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.axonivy.utils.persistence.beans.AuditableIdEntity;

@Entity
@Table(uniqueConstraints = { @UniqueConstraint(name = "UK_DATA_TABLE_STATE_KEY", columnNames = { "STATEKEY" }) })
public class DataTableState extends AuditableIdEntity {
	private static final long serialVersionUID = 1L;

	@Column(nullable = false, unique = true, length = 255)
	private String stateKey;

	@Lob
	@Column(nullable = false)
	private String stateValue;

	public String getStateKey() {
		return stateKey;
	}

	public void setStateKey(String stateKey) {
		this.stateKey = stateKey;
	}

	public String getStateValue() {
		return stateValue;
	}

	public void setStateValue(String stateValue) {
		this.stateValue = stateValue;
	}

	// Builder pattern for convenient, type-safe construction
	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private String stateKey;
		private String stateValue;

		private Builder() {
		}

		public Builder stateKey(String stateKey) {
			this.stateKey = stateKey;
			return this;
		}

		public Builder stateValue(String stateValue) {
			this.stateValue = stateValue;
			return this;
		}

		public DataTableState build() {
			DataTableState s = new DataTableState();
			s.setStateKey(this.stateKey);
			s.setStateValue(this.stateValue);
			return s;
		}
	}

}
