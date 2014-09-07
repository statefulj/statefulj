package org.statefulj.webapp.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cascade;
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
	
	String firstName;
	
	String lastName;
	
	@Column(unique=true)
	String email;
	
	String password;
	
	int token;
	
	@OneToMany(mappedBy="owner", cascade=CascadeType.ALL)
	List<Account> accounts;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
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
	
	public int getToken() {
		return token;
	}

	public void setToken(int token) {
		this.token = token;
	}
	
	public void addAccount(Account account) {
		account.setOwner(this);
		this.accounts.add(account);
	}

	public List<Account> getAccounts() {
		return accounts;
	}

	public void setAccounts(List<Account> accounts) {
		this.accounts = accounts;
	}
}
