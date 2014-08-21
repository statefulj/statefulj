package org.statefulj.framework.model;

public interface ReferenceFactory {
	
	String binder(Class<?> controller, String binder);
	
	String fsmHarness(String controller);
	
	String fsm();

}
