package org.statefulj.webapp.form;

import javax.validation.constraints.Size;

public class RegistrationForm {
	
	@Size(min=1)
	private String firstName;
	
	@Size(min=1)
	private String lastName;
	
	@Size(min=1)
	private String email;
	
	@Size(min=1)
	private String password;
	
	@Size(min=1)
	private String passwordConfirmation;

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

	public String getPasswordConfirmation() {
		return passwordConfirmation;
	}

	public void setPasswordConfirmation(String passwordConfirmation) {
		this.passwordConfirmation = passwordConfirmation;
	}
}
