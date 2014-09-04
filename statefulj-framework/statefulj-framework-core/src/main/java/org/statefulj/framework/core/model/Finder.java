package org.statefulj.framework.core.model;

public interface Finder<T> {

	T find();
	
	T find(Object id);
	
}
