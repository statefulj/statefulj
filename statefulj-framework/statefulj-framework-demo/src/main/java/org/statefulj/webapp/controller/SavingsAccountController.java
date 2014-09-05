package org.statefulj.webapp.controller;

import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.webapp.model.Account;
import org.statefulj.webapp.model.SavingsAccount;

@StatefulController(
	clazz=SavingsAccount.class, 
	startState=Account.NON_EXISTENT,
	factoryId="accountService"
)
public class SavingsAccountController {
	
	@Transition(from=Account.NON_EXISTENT, event="springmvc:post:/accounts/savings", to=Account.ACTIVE)
	public String createSavingsAccount(SavingsAccount savingsAccount, String event) {
		return "redirect:/user";
	}
}
