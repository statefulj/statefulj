package org.statefulj.framework.core.model.impl;

import java.io.Serializable;

import org.springframework.data.repository.CrudRepository;
import org.statefulj.framework.core.model.Finder;

public class CrudRepositoryFinderImpl<T, CT> implements Finder<T, CT> {
	
	CrudRepository<T, Serializable> repo;
	
	public CrudRepositoryFinderImpl(CrudRepository<T, Serializable> repo) {
		this.repo = repo;
	}

	@Override
	public T find(Class<T> clazz, Object id, String event, CT context) {
		return repo.findOne((Serializable)id);
	}

	@Override
	public T find(Class<T> clazz, String event, CT context) {
		return null;
	}

}
