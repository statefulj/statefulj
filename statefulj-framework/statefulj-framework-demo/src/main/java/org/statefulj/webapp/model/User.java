package org.statefulj.webapp.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.statefulj.persistence.jpa.model.StatefulEntity;


@Entity
@Table(name="users")
public class User extends StatefulEntity {
	
	// States
	//
	public static final String UNREGISTERED = "UNREGISTERED";
	public static final String REGISTERED_UNCONFIRMED = "REGISTERED_UNCONFIRMED";
	public static final String REGISTERED_CONFIRMED = "REGISTERED_CONFIRMED";
	public static final String DELETED = "DELETED";

	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	Long id;
	
	@Column(unique=true)
	String email;
	
	String password;
	
	int token;

	public int getToken() {
		return token;
	}

	public void setToken(int token) {
		this.token = token;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
}
