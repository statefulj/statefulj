package org.statefulj.webapp.controller;

import javax.annotation.Resource;

import org.apache.camel.Produce;
import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.core.annotations.Transitions;
import org.statefulj.webapp.form.AccountForm;
import org.statefulj.webapp.messaging.LoanApplicationProducer;
import org.statefulj.webapp.messaging.LoanApplication;
import org.statefulj.webapp.model.Account;
import org.statefulj.webapp.model.LoanAccount;

import static org.statefulj.webapp.model.LoanAccount.*;

import org.statefulj.webapp.services.AccountService;

@StatefulController(
	clazz=LoanAccount.class,
	startState=NON_EXISTENT,
	factoryId="accountService"
)
public class LoanAccountController {
	
	@Resource
	AccountService accountService;
	
	@Produce(uri="vm:loan.application")
	LoanApplicationProducer loanApplicationProducer;
	
	@Transition(from=NON_EXISTENT, event="springmvc:post:/accounts/loan", to=APPROVAL_PENDING)
	public String createAccount(Account account, String event, AccountForm form) {
		
		// Save to database prior to emitting events
		//
		account.setAmount(form.getAmount());
		accountService.save(account);
		
		// Submit the loan for approval
		//
		LoanApplication msg = new LoanApplication();
		msg.setAccountId(account.getId()); // Set the Loan Application Id
		
		loanApplicationProducer.onApplicationSubmitted(msg);
		
		return "redirect:/user";
	}
	
	@Transitions({
		@Transition(from=APPROVAL_PENDING, event="camel:vm:loan.approved", to=ACTIVE),
		@Transition(from=APPROVAL_PENDING, event="camel:vm:loan.rejected", to=REJECTED)
	})
	public void loanReviewed(Account account, String event, LoanApplication msg) {
		String foo = "bar";
	}
}
