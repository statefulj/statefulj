package org.statefulj.webapp.controller;

import javax.annotation.Resource;

import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.webapp.form.AccountForm;
import org.statefulj.webapp.model.Account;
import org.statefulj.webapp.model.LoanAccount;
import org.statefulj.webapp.services.AccountService;

@StatefulController(
	clazz=LoanAccount.class,
	startState=Account.NON_EXISTENT,
	factoryId="accountService"
)
public class LoanAccountController {
	
	@Resource
	AccountService accountService;
	
	@Transition(from=LoanAccount.NON_EXISTENT, event="springmvc:post:/accounts/loan", to=LoanAccount.APPROVAL_PENDING)
	public String createAccount(Account account, String event, AccountForm form) {
		account.setAmount(form.getAmount());
		return "redirect:/user";
	}
}
