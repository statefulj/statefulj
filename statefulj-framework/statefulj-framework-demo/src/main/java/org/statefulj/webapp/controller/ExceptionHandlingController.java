/***
 * 
 * Copyright 2014 Andrew Hall
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.statefulj.webapp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
class ExceptionHandlingController {
	
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