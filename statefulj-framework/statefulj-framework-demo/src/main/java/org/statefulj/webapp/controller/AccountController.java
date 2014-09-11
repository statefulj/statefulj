package org.statefulj.webapp.controller;

import javax.annotation.Resource;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.ModelAndView;
import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.webapp.form.AccountForm;
import org.statefulj.webapp.model.Account;
import static org.statefulj.webapp.model.Account.*;
import org.statefulj.webapp.services.AccountService;

@StatefulController(
	clazz=Account.class,
	startState=NON_EXISTENT,
	factoryId="accountService"
)
public class AccountController {
	
	@Resource
	AccountService accountService;
	
	@Transition(from=NON_EXISTENT, event="springmvc:post:/accounts", to=ACTIVE)
	public String createAccount(Account account, String event, AccountForm form) {
		account.setAmount(form.getAmount());
		return "redirect:/user";
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
