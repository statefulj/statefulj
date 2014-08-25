package org.statefulj.framework.core.model;

import java.io.Serializable;

import org.statefulj.fsm.Persister;

public interface PersistenceSupport<T> {
	
	T find(Serializable id);
	
	Persister<T> getPersister();
}
