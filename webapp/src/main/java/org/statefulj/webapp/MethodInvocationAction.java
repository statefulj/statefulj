package org.statefulj.webapp;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.mutable.MutableObject;
import org.statefulj.fsm.RetryException;
import org.statefulj.fsm.model.Action;

public class MethodInvocationAction implements Action<Object> {

	private Object controller;
	
	private String method;
	
	private Class<?>[] parameters;
	
	@Override
	public void execute(Object entity, String event, Object... parms)
			throws RetryException {
		invoke(entity, event, parms);
	}
	
	@SuppressWarnings("unchecked")
	private void invoke(Object entity, String event, Object... parms) {
		try {
			
			// Remove the first Object in the parm list - it's our Return Value
			//
			ArrayList<Object> parmList = new ArrayList<Object>(Arrays.asList(parms));
			MutableObject<Object> returnValue = (MutableObject<Object>)parmList.remove(0);
			
			// Add the Entity and Event to the pam list to pass to the Controller
			//
			ArrayList<Object> invokeParmList = new ArrayList<Object>(parmList.size() + 2);
			invokeParmList.add(entity);
			invokeParmList.add(event);
			invokeParmList.addAll(parmList);
			
			// Call the method on the Controller
			//
			Method method = controller.getClass().getMethod(this.method, this.parameters);
			returnValue.setValue(method.invoke(this.controller, invokeParmList.toArray()));
		} catch (Exception e) {
			throw new RuntimeException(e);
		} 
	}

	public Object getController() {
		return controller;
	}

	public void setController(Object controller) {
		this.controller = controller;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Class<?>[] getParameters() {
		return parameters;
	}

	public void setParameters(Class<?>[] parameters) {
		this.parameters = parameters;
	}
}
