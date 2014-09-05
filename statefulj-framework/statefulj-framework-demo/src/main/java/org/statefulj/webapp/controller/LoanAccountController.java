package org.statefulj.webapp.controller;

import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.webapp.model.Account;
import org.statefulj.webapp.model.LoanAccount;

@StatefulController(
	clazz=LoanAccount.class, 
	startState=Account.NON_EXISTENT,
	factoryId="accountService"
)
public class LoanAccountController {
	
	@Transition(from=Account.NON_EXISTENT, event="springmvc:post:/accounts/loan", to=LoanAccount.APPROVAL_PENDING)
	public String createLoanAccount(LoanAccount loanAccount, String event) {
		return "redirect:/user";
	}
}
