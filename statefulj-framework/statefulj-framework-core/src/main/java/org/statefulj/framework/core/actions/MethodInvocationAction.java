package org.statefulj.framework.core.actions;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.statefulj.fsm.FSM;
import org.statefulj.fsm.RetryException;
import org.statefulj.fsm.model.Action;

public class MethodInvocationAction implements Action<Object> {

	private final Pattern protocol = Pattern.compile("(([^:]*):)?(.*)");

	private Object controller;
	
	private String method;
	
	private Class<?>[] parameters;

	private FSM<Object> fsm;

	public void execute(Object stateful, String event, Object... parms) throws RetryException {
		invoke(stateful, event, parms);
	}
	
	@SuppressWarnings("unchecked")
	private void invoke(Object entity, String event, Object... parms) {
		try {
			
			// Remove the first Object in the parm list - it's our Return Value
			//
			ArrayList<Object> parmList = new ArrayList<Object>(Arrays.asList(parms));
			MutableObject<Object> returnValue = (MutableObject<Object>)parmList.remove(0);
			
			// Add the Entity and Event to the pam list to pass to the Controller
			// TODO : Inspect method signature - make entity and event optional
			//
			ArrayList<Object> invokeParmList = new ArrayList<Object>(parmList.size() + 2);
			invokeParmList.add(entity);
			invokeParmList.add(event);
			invokeParmList.addAll(parmList);
			
			// Call the method on the Controller
			// TODO : Add test case
			//
			Method method = controller.getClass().getMethod(this.method, this.parameters);
			Object[] methodParms = invokeParmList.subList(0, this.parameters.length).toArray();
			Object retVal = method.invoke(this.controller, methodParms);
			if (retVal instanceof String) {
				Pair<String, String> pair = this.parseResponse((String)retVal);
				if ("event".equals(pair.getLeft())) {
					this.fsm.onEvent(entity, pair.getRight(), returnValue, parms);
				} else {
					returnValue.setValue(retVal);
				}
			} else {
				returnValue.setValue(retVal);
			}
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
	
	public FSM<?> getFsm() {
		return fsm;
	}

	public void setFsm(FSM<Object> fsm) {
		this.fsm = fsm;
	}

	public String toString() {
		return this.method;
	}

	private Pair<String, String> parseResponse(String response) {
		Matcher matcher = this.protocol.matcher(response);
		if (!matcher.matches()) {
			throw new RuntimeException("Unable to parse response=" + response);
		}
		return new ImmutablePair<String, String>(matcher.group(2), matcher.group(3));
	}
}
