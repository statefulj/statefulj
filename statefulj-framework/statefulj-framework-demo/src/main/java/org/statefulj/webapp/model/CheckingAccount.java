package org.statefulj.webapp.model;

import javax.persistence.Entity;

@Entity
public class CheckingAccount extends Account {
	
	public String getType() {
		return "Checking";
	}

}
