package org.statefulj.webapp.controller;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.core.annotations.Transitions;
import org.statefulj.fsm.TooBusyException;
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
	
	@Transition(from=User.UNREGISTERED, event="springmvc:/login")
	public String loginPage() {
 		return "login";
	}
	
	@Transition(from=User.UNREGISTERED, event="springmvc:/registration")
	public String registrationPage() {
 		return "registration";
	}
	
	@Transitions({
		@Transition(from=User.REGISTERED_UNCONFIRMED, event="successful-confirmation", to=User.REGISTERED_CONFIRMED),
		@Transition(event="springmvc:/login"),
		@Transition(event="springmvc:/registration")
	})
	public String redirectToAccount() {
 		return "redirect:/user";
	}

	// TODO : Fix - we shouldn't have to require passing in the RequestParam name
	@Transition(from=User.UNREGISTERED, event="springmvc:post:/user/register", to=User.REGISTERED_UNCONFIRMED)
	public String newUser(
			User user, 
			String event, 
			HttpServletRequest request, 
			@RequestParam("email") String email, 
			@RequestParam("password") String password) {
		
		// Initialize and save the User
		//
		user.setEmail(email);
		user.setPassword(password); 
		user.setToken(RandomUtils.nextInt(1, 99999999));
		userService.save(user);
		
		// Login the newly registered User
		//
		userSessionService.login(request.getSession(), user);
		
		// Redirect to user page
		//
		return "redirect:/user";
	
	}
	
	@Transition(from=User.REGISTERED_UNCONFIRMED, event="springmvc:/user")
	public ModelAndView confirmationPage(User user) {
		ModelAndView mv = new ModelAndView("confirmation");
		mv.addObject("user", user);
		return mv;
	}
	
	@Transition(event="springmvc:/user")
	public ModelAndView userDetail(User user, String event) {
		ModelAndView mv = new ModelAndView("user");
		mv.addObject("user", user);
		mv.addObject("event", event);
		return mv;
	}
	
	@Transition(from=User.REGISTERED_UNCONFIRMED, event="springmvc:post:/user/confirmation")
	public String confirmUser(
			User user, 
			String event, 
			@RequestParam("token") int token) throws InstantiationException, TooBusyException {
		if (user.getToken() == token) {
			return "event:successful-confirmation";
		} else {
			return "redirect:/user?msg=bad+token";
		}
	}
}
