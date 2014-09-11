package org.statefulj.webapp.model;

import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity
public class CheckingAccount extends Account {
	
	public final static String TYPE = "Checking";
	
	@Transient
	public String getType() {
		return TYPE;
	}

}
