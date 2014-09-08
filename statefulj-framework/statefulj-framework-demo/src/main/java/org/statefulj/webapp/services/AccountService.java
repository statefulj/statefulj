package org.statefulj.webapp.services;

import org.statefulj.webapp.model.Account;

public interface AccountService  {
	
	Account find(Long Id);
	
	void addAccount(Account account);
	
	void save(Account account);
}
