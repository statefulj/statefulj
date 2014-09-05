package org.statefulj.webapp.controller;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.core.annotations.Transitions;
import org.statefulj.webapp.model.User;
import org.statefulj.webapp.services.UserService;
import org.statefulj.webapp.services.UserSessionService;

@StatefulController(
	clazz=User.class, 
	startState=User.UNREGISTERED,
	finderId="userSessionService"
)
public class UserController {
	 
	@Resource
	UserService userService;
	
	@Resource
	UserSessionService userSessionService;
	
	// -- UNREGISTERED -- //
	
	@Transition(from=User.UNREGISTERED, event="springmvc:/")
	public String homePage() {
 		return "index";
	}
	
	@Transition(from=User.UNREGISTERED, event="springmvc:/login")
	public String loginPage() {
 		return "login";
	}
	
	@Transition(from=User.UNREGISTERED, event="springmvc:/registration")
	public String registrationPage() {
 		return "registration";
	}
	
	// TODO : Fix - we shouldn't have to require passing in the RequestParam name
	@Transition(from=User.UNREGISTERED, event="springmvc:post:/user/register", to=User.REGISTERED_UNCONFIRMED)
	public String newUser(
			User user, 
			String event, 
			HttpServletRequest request, 
			@RequestParam("email") String email, 
			@RequestParam("password") String password) {
		
		// Create the User
		//
		user.setEmail(email);
		user.setPassword(password); 
		user.setToken(RandomUtils.nextInt(1, 99999999));
		userService.save(user);
		
		// Login the newly registered User
		//
		userSessionService.login(request.getSession(), user);
		
		// Redirect to confirmation page
		//
		return "redirect:/confirmation";
	}
	
	// -- REGISTERED_UNCONFIRMED -- //

	@Transition(from=User.REGISTERED_UNCONFIRMED, event="springmvc:/user")
	public ModelAndView redirectToConfirmation(User user) {
		return new ModelAndView("redirect:/confirmation");
	}
	
	@Transition(from=User.REGISTERED_UNCONFIRMED, event="springmvc:/confirmation")
	public ModelAndView confirmationPage(User user) {
		ModelAndView mv = new ModelAndView("confirmation");
		mv.addObject("user", user);
		return mv;
	}
	
	@Transition(from=User.REGISTERED_UNCONFIRMED, event="springmvc:post:/user/confirmation")
	public String confirmUser(
			User user, 
			String event, 
			@RequestParam("token") int token) {
		
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
		@Transition(from=User.REGISTERED_UNCONFIRMED, event="successful-confirmation", to=User.REGISTERED_CONFIRMED),
		
		// If we're logged in, don't display either login or registration pages
		//
		@Transition(event="springmvc:/"),
		@Transition(event="springmvc:/login"),
		@Transition(event="springmvc:/registration")
	})
	public String redirectToAccount() {
 		return "redirect:/user";
	}

	// -- REGISTERED_CONFIRMED -- //

	@Transition(from=User.REGISTERED_CONFIRMED, event="springmvc:/user")
	public ModelAndView userDetail(User user, String event) {
		ModelAndView mv = new ModelAndView("user");
		mv.addObject("user", user);
		mv.addObject("event", event);
		return mv;
	}

	@Transition(from=User.REGISTERED_CONFIRMED, event="springmvc:/user/delete", to=User.DELETED)
	public String deleteUser(User user, String event) {
		return "redirect:/logout";
	}
}
