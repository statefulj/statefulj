package org.statefulj.framework;

import java.util.HashMap;
import java.util.Map;

import org.statefulj.framework.model.EndpointBinder;

public class BinderRepo {
	
	private static Map<String, EndpointBinder> binders = new HashMap<String, EndpointBinder>();
	
	public static void put(String key, EndpointBinder binder) {
		binders.put(key, binder);
	}
	
	public static EndpointBinder get(String key) {
		return binders.get(key);
	}

}
