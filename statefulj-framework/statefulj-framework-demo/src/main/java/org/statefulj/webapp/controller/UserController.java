package org.statefulj.webapp.controller;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.core.annotations.Transitions;
import org.statefulj.webapp.model.User;
import org.statefulj.webapp.repo.UserRepository;
import org.statefulj.webapp.services.UserService;

@StatefulController(
	clazz=User.class, 
	startState=UserController.UNREGISTERED,
	finderId="userService"
)
public class UserController {
	
	// States
	//
	static final String UNREGISTERED = "UNREGISTERED";
	static final String REGISTERED_UNCONFIRMED = "REGISTERED_UNCONFIRMED";
	static final String REGISTERED_CONFIRMED = "REGISTERED_CONFIRMED";

	@Resource
	UserService userService;
	
	// Transitions/Actions
	//
	@Transition(from=UNREGISTERED, event="springmvc:post:/user/register", to=REGISTERED_UNCONFIRMED)
	public String newUser(User user, String event, HttpServletRequest request, @RequestParam(value="email") String email, @RequestParam(value="password") String password) {
		user.setEmail(email);
		user.setPassword(password);  
		userService.save(user);
		userService.login(request.getSession(), user);
		return "redirect:/user";
	}
	
	@Transition(event="springmvc:/user")
	public ModelAndView userDetail(User user, String event) {
		return userView(user, event);
	}
	
	private ModelAndView userView(User user, String event) {
		ModelAndView mv = new ModelAndView("user");
		mv.addObject("user", user);
		mv.addObject("event", event);
		return mv;
	}
}
