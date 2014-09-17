package org.statefulj.webapp.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.statefulj.persistence.jpa.model.StatefulEntity;

@Entity
public class Notification extends StatefulEntity {
	
	// States
	//
	public static final String NON_EXISTENT = "NON_EXISTENT";
	public static final String SHOWING = "SHOWING";
	public static final String DELETED = "DELETED";
	
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	Long id;
	
	@ManyToOne(optional=false)
	User owner;
	
	String type;
	
	String message;

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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
