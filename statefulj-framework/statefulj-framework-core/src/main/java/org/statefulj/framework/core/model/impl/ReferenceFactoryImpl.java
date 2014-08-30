package org.statefulj.framework.core.model.impl;

import static java.beans.Introspector.decapitalize;

import java.beans.Introspector;
import java.lang.reflect.Method;

import org.statefulj.framework.core.model.ReferenceFactory;

public class ReferenceFactoryImpl implements ReferenceFactory {

	private String ctrl;
	
	public ReferenceFactoryImpl(String ctrl) {
		this.ctrl = ctrl;
	}

	@Override
	public String getBinderId() {
		return decapitalize(ctrl + ".binder");
	}

	@Override
	public String getFinderId() {
		return decapitalize(ctrl + ".finder");
	}

	@Override
	public String getHarnessId() {
		return decapitalize(ctrl + ".harness");
	}

	@Override
	public String getPersisterId() {
		return decapitalize(ctrl + ".persister");
	}
	
	@Override
	public String getFactoryId() {
		return decapitalize(ctrl + ".factory");
	}
	
	@Override
	public String getFSMId() {
		return decapitalize(ctrl + ".fsm");
	}
	
	@Override
	public String getStateId(String state) {
		return decapitalize(ctrl + ".state." + state);
	}
	
	@Override
	public String getTransitionId(int cnt) {
		return decapitalize(ctrl + ".transition." + cnt);
	}
	
	@Override
	public String getActionId(Method method) {
		return decapitalize(ctrl + ".action." + method.getName());
	}
}
