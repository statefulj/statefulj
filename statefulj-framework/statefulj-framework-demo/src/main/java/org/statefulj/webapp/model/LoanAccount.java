package org.statefulj.webapp.model;

import javax.persistence.Entity;

@Entity
public class LoanAccount extends Account {
	
	public static final String APPROVAL_PENDING = "APPROVAL_PENDING";
	
	public String getType() {
		return "Loan";
	}

}
