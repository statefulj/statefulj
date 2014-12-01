package org.statefulj.framework.binders.jersey;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class BindingsRegistry {
	
	private static Map<String, List<Class<?>>> bindingsMap = new HashMap<String, List<Class<?>>>();
	
	static void addBinding(Class<?> binding) {
		String pkg = binding.getPackage().getName();
		List<Class<?>> bindings = bindingsMap.get(pkg);
		if (bindings == null) {
			bindings = new LinkedList<Class<?>>();
			bindingsMap.put(pkg, bindings);
		}
		bindings.add(binding);
	}
	
	static Map<String, List<Class<?>>> getBindingsMap() {
		return bindingsMap;
	}

	static List<Class<?>> getAllBindings() {
		List<Class<?>> allBindings = new LinkedList<Class<?>>();
		for(List<Class<?>> bindings : bindingsMap.values()) {
			allBindings.addAll(bindings);
		}
		return allBindings;
	}

	static List<Class<?>> getBindingsByPkg(String pkg) {
		return bindingsMap.get(pkg);
	}

}
