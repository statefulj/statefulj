package org.statefulj.webapp.model;

import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity
public class LoanAccount extends Account {
	
	public final static String TYPE = "Loan";
	
	@Transient
	public String getType() {
		return TYPE;
	}

}
