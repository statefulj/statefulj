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
package org.statefulj.webapp.services.impl;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.statefulj.framework.core.model.Factory;
import org.statefulj.webapp.model.Account;
import org.statefulj.webapp.model.CheckingAccount;
import org.statefulj.webapp.model.LoanAccount;
import org.statefulj.webapp.model.SavingsAccount;
import org.statefulj.webapp.model.User;
import org.statefulj.webapp.repo.AccountRepository;
import org.statefulj.webapp.services.AccountService;
import org.statefulj.webapp.services.UserSessionService;

@Service("accountService")
@Transactional
public class AccountServiceImpl implements AccountService, Factory<Account, HttpServletRequest> {
	
	@Resource
	AccountRepository accountRepo;

	@Resource
	UserSessionService userSessionService;
	
	@PersistenceContext
	EntityManager entityManager;
	
	@Override
	public Account find(Long Id) {
		return accountRepo.findOne(Id);
	}

	@Override
	public void addAccount(Account account) {
		User user = this.userSessionService.findLoggedInUser();
		user.addAccount(account);
	}

	@Override
	public Account create(Class<Account> clazz, String event, HttpServletRequest request) {
		Account account = null;
		
		switch(request.getParameter("type")) {
		
			case "checking" :
				account = new CheckingAccount();
				break;
		
			case "savings" :
				account = new SavingsAccount();
				break;
				
			case "loan" :
				account = new LoanAccount();
				break;
				
			default :
				throw new RuntimeException("Unrecognized account type " + request.getParameter("type"));
		}
		
		addAccount(account);
		return account;
	}

	@Override
	public void save(Account account) {
		this.accountRepo.save(account);
		this.entityManager.flush();
	}

}
