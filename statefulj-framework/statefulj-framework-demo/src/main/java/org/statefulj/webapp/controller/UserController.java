package org.statefulj.webapp.controller;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.core.annotations.Transitions;
import org.statefulj.webapp.form.RegistrationForm;
import org.statefulj.webapp.model.User;
import static org.statefulj.webapp.model.User.*;
import org.statefulj.webapp.services.UserService;
import org.statefulj.webapp.services.UserSessionService;

@StatefulController(
	clazz=User.class, 
	startState=UNREGISTERED,
	finderId="userSessionService"
)
public class UserController {
	 
	@Resource
	UserService userService;
	
	@Resource
	UserSessionService userSessionService;
	
	@PersistenceContext
	EntityManager entityManager;
	
	// -- UNREGISTERED -- //
	
	@Transition(from=UNREGISTERED, event="springmvc:/")
	public String homePage() {
 		return "index";
	}
	
	@Transition(from=UNREGISTERED, event="springmvc:/login")
	public String loginPage() {
 		return "login";
	}
	
	@Transition(from=UNREGISTERED, event="springmvc:/registration")
	public String registrationPage() {
 		return "registration";
	}
	
	@Transition(from=UNREGISTERED, event="springmvc:post:/registration", to=REGISTERED_UNCONFIRMED)
	public String newUser(
			User user, 
			String event, 
			HttpServletRequest request, 
			@Valid RegistrationForm regForm,
			BindingResult result,
			Model model) {
		
		// If the Registration Form is invalid, display the Registration Form
		//
		if (result.hasErrors() || !regForm.getPassword().equals(regForm.getPasswordConfirmation())) {
			model.addAttribute("message", "Ooops... Try again");
			model.addAttribute("reg", regForm);
			return "registration";
		} else {
			
			// Copy from Registration Form to the User
			//
			user.setFirstName(regForm.getFirstName());
			user.setLastName(regForm.getLastName());
			user.setEmail(regForm.getEmail());
			user.setPassword(regForm.getPassword());
			user.setToken(RandomUtils.nextInt(1, 99999999));

			// Save and flush to db - if there is a problem, fail before logging in
			//
			try {
				userService.save(user);
				entityManager.flush();  
			} catch(Exception e) {
				if (e.getCause() instanceof ConstraintViolationException) {
					throw new DuplicateUserException();
				} else {
					throw e;
				}
			}
			
			// Login the newly registered User
			//
			userSessionService.login(request.getSession(), user);
			
			// Redirect to confirmation page
			//
			return "redirect:/confirmation";
		}
	}
	
	// -- REGISTERED_UNCONFIRMED -- //

	@Transition(from=REGISTERED_UNCONFIRMED, event="springmvc:/user")
	public String redirectToConfirmation(User user) {
		return "redirect:/confirmation";
	}
	
	@Transition(from=REGISTERED_UNCONFIRMED, event="springmvc:/confirmation")
	public String confirmationPage(User user, String event, Model model) {
		model.addAttribute("user", user);
		return "confirmation";
	}
	
	@Transition(from=REGISTERED_UNCONFIRMED, event="springmvc:post:/user/confirmation")
	public String confirmUser(
			User user, 
			String event, 
			@RequestParam int token) {
		
		// If a valid token, emit a "successful-confirmation" event, this will
		// transition the User into a REGISTERED_CONFIRMED state
		//
		if (user.getToken() == token) {
			return "event:successful-confirmation";
		} else {
			return "redirect:/user?msg=bad+token";
		}
	}

	@Transitions({
		// If we get a "successful-confirmation" event, transition into REGISTERD_CONFIRMED
		//
		@Transition(from=REGISTERED_UNCONFIRMED, event="successful-confirmation", to=REGISTERED_CONFIRMED),
		
		// If we're logged in, don't display either login or registration pages
		//
		@Transition(event="springmvc:/"),
		@Transition(event="springmvc:/login"),
		@Transition(event="springmvc:/registration")
	})
	public String redirectToUser() {
 		return "redirect:/user";
	}

	@Transition(from=REGISTERED_CONFIRMED, event="springmvc:/confirmation")
	public String redirectToUser(User user, String event, Model model) {
 		return "redirect:/user";
	}

	// -- REGISTERED_CONFIRMED -- //

	@Transition(from=REGISTERED_CONFIRMED, event="springmvc:/user")
	public String userPage(User user, String event, Model model) {
		model.addAttribute("user", user);
		model.addAttribute("event", event);
		return "user";
	}

	@Transitions({
		@Transition(from=REGISTERED_CONFIRMED, event="springmvc:/accounts/loan"),
		@Transition(from=REGISTERED_CONFIRMED, event="springmvc:/accounts/savings"),
		@Transition(from=REGISTERED_CONFIRMED, event="springmvc:/accounts/checking")
	})
	public String createAccountForm(User user, String event, Model model) {
		String createAccountUri = (event.equals("/accounts/loan")) ? "/accounts/loan" : "/accounts";
		String[] parts = event.split("/");
		String type = parts[2];
		String typeTitle = WordUtils.capitalize(type);
		
		model.addAttribute("createAccountUri", createAccountUri);
		model.addAttribute("type", type);
		model.addAttribute("typeTitle", typeTitle);
		
		return "createAccount";
	}

	@Transition(from=REGISTERED_CONFIRMED, event="springmvc:/user/delete", to=DELETED)
	public String deleteUser(User user, String event) {
		return "redirect:/logout";
	}
	
	// -- Error Handling -- //

	@ExceptionHandler(DuplicateUserException.class)
	public ModelAndView handleError(DuplicateUserException e) {
		ModelAndView mv = new ModelAndView();
		mv.getModel().put("message", "Ooops... That User is already registered.  Try a different email");
		mv.getModel().put("reg", new RegistrationForm());
		return mv;
	}
}
