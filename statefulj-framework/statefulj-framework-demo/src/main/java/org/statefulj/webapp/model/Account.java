package org.statefulj.webapp.model;

import java.math.BigDecimal;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.statefulj.persistence.jpa.model.StatefulEntity;


@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public abstract class Account extends StatefulEntity {
	
	// States
	//
	public static final String NON_EXISTENT = "NON_EXISTENT";
	public static final String ACTIVE = "ACTIVE";
	public static final String DELETED = "DELETED";
	public static final String APPROVAL_PENDING = "APPROVAL_PENDING";
	public static final String REJECTED = "REJECTED";
	
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	Long id;
	
	@ManyToOne(optional=false)
	User owner;
	
	BigDecimal amount;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	
	@Transient
	public abstract String getType();

	public String toString() {
		return getType() + ": state=" + getState();
	}
}
