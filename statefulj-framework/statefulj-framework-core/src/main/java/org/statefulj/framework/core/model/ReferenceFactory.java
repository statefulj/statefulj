package org.statefulj.framework.core.model;

import java.lang.reflect.Method;

public interface ReferenceFactory {
	
	String getBinderId();

	String getFinderId();
	
	String getFSMHarnessId(); 

	String getPersisterId(); 

	String getFactoryId();

	String getFSMId();
	
	String getStateId(String state);
	
	String getTransitionId(int cnt);
	
	String getActionId(Method method);

}
