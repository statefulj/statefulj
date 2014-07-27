package org.statefulj.webapp;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import org.statefulj.webapp.model.User;
import org.statefulj.webapp.repo.UserRepository;

@StatefulController(clazz=User.class, startState=UserController.NEW_STATE)
public class UserController {
	
	// States
	//
	public static final String NEW_STATE = "new";
	public static final String NOT_NEW_STATE = "not-new";
	public static final String BOO_STATE = "boo";
	public static final String WHATEVER_STATE = "whatever";

	@Autowired
	UserRepository userRepository;
	
	// Transitions/Actions
	//
	@Transition(from=NEW_STATE, event="/{id}/new", to=NOT_NEW_STATE)
	public ModelAndView newUser(User user, String event) {
		ModelAndView mv = new ModelAndView("new");
		mv.addObject("id", user.getId());
		return mv;
	}

	@Transition(from=NOT_NEW_STATE, event="/{id}/boo", to=BOO_STATE)
	public String boo(User user, String event) {
		return "boo";
	}

	@Transition(from=NOT_NEW_STATE, event="/{id}/whatever", to=WHATEVER_STATE)
	public String whatever(User user, String event) {
		return "whatever";
	}

	@Transition(from=BOO_STATE, event="/{id}/zoo", to=BOO_STATE)
	public String zoo(User user, String event, HttpServletRequest request, @PathVariable(value="id")String id) {
		return "zoo";
	}
}
