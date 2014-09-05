package org.statefulj.webapp.services.impl;

import javax.annotation.Resource;
import javax.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.statefulj.webapp.model.Account;
import org.statefulj.webapp.model.User;
import org.statefulj.webapp.repo.AccountRepository;
import org.statefulj.webapp.services.AccountService;
import org.statefulj.webapp.services.UserSessionService;

@Service("accountService")
@Transactional
public class AccountServiceImpl implements AccountService {
	
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

}
