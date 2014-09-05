package org.statefulj.webapp.model;

import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity
public class SavingsAccount extends Account {
	
	@Transient
	public String getType() {
		return "Savings";
	}

}
