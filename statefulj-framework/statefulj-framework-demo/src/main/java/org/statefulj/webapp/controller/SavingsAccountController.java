package org.statefulj.webapp.controller;

import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.webapp.model.Account;
import org.statefulj.webapp.model.SavingsAccount;

@StatefulController(
	clazz=SavingsAccount.class, 
	startState=Account.NON_EXISTENT
)
public class SavingsAccountController extends AccountController {
	
	@Transition(from=Account.NON_EXISTENT, event="springmvc:post:/accounts/savings", to=Account.ACTIVE)
	public String createSavingsAccount(SavingsAccount savingsAccount, String event) {
		addAccount(savingsAccount);
		return "redirect:/user";
	}
}
