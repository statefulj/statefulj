/***
 * 
 * Copyright 2014 Andrew Hall
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.statefulj.webapp.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.statefulj.persistence.jpa.model.StatefulEntity;


@Entity
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

	@OneToMany(mappedBy="owner", cascade=CascadeType.ALL)
	List<Notification> notifications;

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

	public void addNotification(Notification notification) {
		notification.setOwner(this);
		this.notifications.add(notification);
	}
	
	public List<Notification> getNotifications() {
		return notifications;
	}

	public void setNotifications(List<Notification> notifications) {
		this.notifications = notifications;
	}
}
