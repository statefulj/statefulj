package org.statefulj.webapp.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.statefulj.webapp.form.RegistrationForm;

@ControllerAdvice
public class ExceptionHandlingController {

  @ExceptionHandler(DuplicateUserException.class)
  public ModelAndView handleError(DuplicateUserException e) {
	  ModelAndView mv = new ModelAndView();
	mv.getModel().put("message", "Ooops... That User is already registered.  Try a different email");
	mv.getModel().put("reg", new RegistrationForm());
	return mv;
  }
}