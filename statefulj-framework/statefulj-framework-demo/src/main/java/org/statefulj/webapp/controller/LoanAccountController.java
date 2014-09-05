package org.statefulj.webapp.controller;

import javax.annotation.Resource;

import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.webapp.model.Account;
import org.statefulj.webapp.model.LoanAccount;
import org.statefulj.webapp.services.AccountService;

@StatefulController(
	clazz=LoanAccount.class, 
	startState=Account.NON_EXISTENT
)
public class LoanAccountController {
	
	@Resource
	AccountService accountService;
	
	@Transition(from=Account.NON_EXISTENT, event="springmvc:post:/accounts/loan", to=LoanAccount.APPROVAL_PENDING)
	public String createLoanAccount(LoanAccount loanAccount, String event) {
		accountService.addAccount(loanAccount);
		return "redirect:/user";
	}
}
