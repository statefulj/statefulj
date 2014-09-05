package org.statefulj.webapp.services.impl;

import javax.annotation.Resource;
import javax.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.statefulj.framework.core.model.Factory;
import org.statefulj.webapp.model.Account;
import org.statefulj.webapp.model.User;
import org.statefulj.webapp.repo.AccountRepository;
import org.statefulj.webapp.services.AccountService;
import org.statefulj.webapp.services.UserSessionService;

@Service("accountService")
@Transactional
public class AccountServiceImpl implements AccountService, Factory<Account> {
	
	@Resource
	AccountRepository accountRepo;

	@Resource
	UserSessionService userSessionService;
	
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
	public Account create(Class<Account> clazz) {
		try {
			User user = userSessionService.findLoggedInUser();
			Account account = clazz.newInstance();
			user.addAccount(account);
			return account;
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
