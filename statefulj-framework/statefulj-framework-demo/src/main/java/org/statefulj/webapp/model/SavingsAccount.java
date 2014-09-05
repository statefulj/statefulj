package org.statefulj.webapp.model;

import javax.persistence.Entity;

@Entity
public class SavingsAccount extends Account {
	
	public String getType() {
		return "Savings";
	}

}
