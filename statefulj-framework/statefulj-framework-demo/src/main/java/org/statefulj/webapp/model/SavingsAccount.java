package org.statefulj.webapp.model;

import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity
public class SavingsAccount extends Account {
	
	public final static String TYPE = "Savings";
	
	@Transient
	public String getType() {
		return TYPE;
	}

}
