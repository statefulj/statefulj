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
package org.statefulj.framework.tests.controllers;


import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.tests.model.User;
import static org.statefulj.framework.tests.model.User.*;

@StatefulController(
	value="overloadedMethodController",
	clazz=User.class, 
	startState=ONE_STATE
)
public class OverloadedMethodController {
	
	@Transition(event="one")
	public String method(User user, String event) {
		return "method1";
	}
	
	@Transition(event="two")
	public String method(User user, String event, String parm) {
		return "method2";
	}
	
	@Transition(event="three")
	public String method(User user, String event, Integer parm) {
		return "method3";
	}
}
