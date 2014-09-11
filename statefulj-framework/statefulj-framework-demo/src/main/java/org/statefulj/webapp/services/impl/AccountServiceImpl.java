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
