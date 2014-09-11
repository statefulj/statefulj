package org.statefulj.webapp.controller;

import javax.annotation.Resource;

import org.apache.camel.Produce;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.ModelAndView;
import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.core.annotations.Transitions;
import org.statefulj.webapp.form.AccountForm;
import org.statefulj.webapp.messaging.AccountApplication;
import org.statefulj.webapp.messaging.AccountApplicationReviewer;
import org.statefulj.webapp.model.Account;

import static org.statefulj.webapp.model.Account.*;
import static org.statefulj.webapp.rules.AccountRules.*;

import org.statefulj.webapp.services.AccountService;

@StatefulController(
	clazz=Account.class,
	startState=NON_EXISTENT,
	factoryId="accountService"
)
public class AccountController {
	
	// EVENTS
	//
	final String ACCOUNT_APPROVED_EVENT = "camel:" + ACCOUNT_APPROVED;
	final String ACCOUNT_REJECTED_EVENT = "camel:" + ACCOUNT_REJECTED;
	
	@Resource
	AccountService accountService;
	
	@Produce(uri=REVIEW_APPLICATION)
	AccountApplicationReviewer applicationReviewer;
	
	@Transition(from=NON_EXISTENT, event="springmvc:post:/accounts", to=APPROVAL_PENDING)
	public String createAccount(Account account, String event, AccountForm form) {
		
		// Save to database prior to emitting events
		//
		account.setAmount(form.getAmount());
		accountService.save(account);
		
		// Submit the Account Application for approval
		//
		AccountApplication application = new AccountApplication();
		application.setAccountId(account.getId()); // Set the Loan Application Id
		application.setType(account.getType());
		
		applicationReviewer.submitForApproval(application);
		
		return "redirect:/user";
	}

	@Transitions({
		@Transition(from=APPROVAL_PENDING, event=ACCOUNT_APPROVED_EVENT, to=ACTIVE),
		@Transition(from=APPROVAL_PENDING, event=ACCOUNT_REJECTED_EVENT, to=REJECTED)
	})
	public void accountReviewed(Account account, String event, AccountApplication msg) {
		String foo = "bar";
	}
	
	// Make sure that only the owner can access the account
	//
	@Transition(event="springmvc:/accounts/{id}")
	@PreAuthorize("#account.owner.email == principal.username")
	public ModelAndView displayAccount(Account account, String event) {
		ModelAndView mv = new ModelAndView("account");
		mv.addObject("account", account);
		return mv;
	}
}
