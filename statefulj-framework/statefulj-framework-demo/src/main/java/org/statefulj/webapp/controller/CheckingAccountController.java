package org.statefulj.webapp.controller;

import javax.annotation.Resource;

import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.webapp.model.Account;
import org.statefulj.webapp.model.CheckingAccount;
import org.statefulj.webapp.services.AccountService;

@StatefulController(
	clazz=CheckingAccount.class, 
	startState=Account.NON_EXISTENT
)
public class CheckingAccountController {
	
	@Resource
	AccountService accountService;
	
	@Transition(from=Account.NON_EXISTENT, event="springmvc:post:/accounts/checking", to=Account.ACTIVE)
	public String createCheckingAccount(CheckingAccount checkingAccount, String event) {
		accountService.addAccount(checkingAccount);
		return "redirect:/user";
	}
}
