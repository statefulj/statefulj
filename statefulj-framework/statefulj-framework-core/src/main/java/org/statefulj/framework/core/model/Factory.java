package org.statefulj.framework.core.model;

public interface Factory<T> {
	
	// TODO : Pass the class into the method so that we can deal with subclassing
	T create();

}
