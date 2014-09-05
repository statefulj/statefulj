package org.statefulj.webapp.services.impl;

import javax.annotation.Resource;
import javax.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.statefulj.webapp.model.Account;
import org.statefulj.webapp.repo.AccountRepository;
import org.statefulj.webapp.services.AccountService;

@Service("accountService")
@Transactional
public class AccountServiceImpl implements AccountService {
	
	@Resource
	AccountRepository accountRepo;

	@Override
	public Account find(Long Id) {
		return accountRepo.findOne(Id);
	}

}
