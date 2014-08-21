package org.statefulj.framework.model.impl;

import java.beans.Introspector;

import org.statefulj.framework.model.ReferenceFactory;

public class ReferenceFactoryImpl implements ReferenceFactory {

	public static String MVC_SUFFIX = "MVCProxy";
	public static String FSM_SUFFIX = "FSM";
	public static String FSM_HARNESS_SUFFIX = "FSMHarness";
	public static String STATE_SUFFIX = "State";

	@Override
	public String binder(Class<?> controller, String binder) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String fsmHarness(String controller) {
		return Introspector.decapitalize(controller + FSM_HARNESS_SUFFIX);
	}

	@Override
	public String fsm() {
		// TODO Auto-generated method stub
		return null;
	}

}
