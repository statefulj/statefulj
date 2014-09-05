package org.statefulj.webapp.services;

import org.statefulj.webapp.model.Account;
import org.statefulj.webapp.model.CheckingAccount;
import org.statefulj.webapp.model.User;

public interface AccountService  {
	
	Account find(Long Id);
	
}
