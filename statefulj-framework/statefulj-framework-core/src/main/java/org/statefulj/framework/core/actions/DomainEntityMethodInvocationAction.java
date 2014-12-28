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
package org.statefulj.framework.core.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.statefulj.fsm.FSM;
import org.statefulj.fsm.RetryException;

public class DomainEntityMethodInvocationAction extends MethodInvocationAction {

	public DomainEntityMethodInvocationAction(
			String method,
			Class<?>[] parameters,
			FSM<Object> fsm) {
		super(method, parameters, fsm, null);
	}
	
	@Override
	protected Object invoke(Object stateful, String event, List<Object> invokeParmList) throws RetryException, SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		return invoke(stateful, invokeParmList);
	}
	
	@Override
	protected List<Object> buildInvokeParameters(Object stateful, String event, List<Object> parmList) {

		// Add the Entity and Event to the parm list to pass to the Controller
		// TODO : Inspect method signature - make entity and event optional
		//
		ArrayList<Object> invokeParmList = new ArrayList<Object>(parmList.size() + 1);
		invokeParmList.add(event);
		invokeParmList.addAll(parmList);
		
		return invokeParmList;
	}
}
