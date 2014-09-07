package org.statefulj.webapp.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class ExceptionHandlingController {

	@ExceptionHandler(Exception.class)
    public String oops(Exception e) {
        return "oops";
    }
	
	@ExceptionHandler(NoHandlerFoundException.class)
    public String notFound (NoHandlerFoundException e) {
        return "notFound";
    }
	
}