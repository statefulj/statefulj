package org.statefulj.framework.core.model.impl;

import java.io.Serializable;

import org.springframework.data.repository.CrudRepository;
import org.statefulj.framework.core.model.Finder;

public class CrudRepositoryFinderImpl<T> implements Finder<T> {
	
	CrudRepository<T, Serializable> repo;
	
	public CrudRepositoryFinderImpl(CrudRepository<T, Serializable> repo) {
		this.repo = repo;
	}

	@Override
	public T find(Object id) {
		return repo.findOne((Serializable)id);
	}

	@Override
	public T find() {
		return null;
	}

}
