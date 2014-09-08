package org.statefulj.webapp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class ExceptionHandlingController {
	
	private Logger logger = LoggerFactory.getLogger(ExceptionHandlingController.class);

	@ExceptionHandler(Exception.class)
    public String oops(Exception e) {
		logger.error("Unhandled Exception", e);
        return "oops";
    }
	
	@ExceptionHandler(NoHandlerFoundException.class)
    public String notFound(NoHandlerFoundException e) {
        return "notFound";
    }
	
}