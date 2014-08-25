package org.statefulj.framework.core.model.impl;

import java.io.Serializable;

import org.springframework.data.repository.CrudRepository;
import org.statefulj.framework.core.model.PersistenceSupport;
import org.statefulj.fsm.Persister;

public class PersistenceSupportImpl<T> implements PersistenceSupport<T> {
	
	private CrudRepository<T, Serializable> repo;
	
	private Persister<T> persister;
	
	PersistenceSupportImpl(CrudRepository<T, Serializable> repo, Persister<T> persister) {
		this.repo = repo;
		this.persister = persister;
	}

	public CrudRepository<T, Serializable> getRepo() {
		return repo;
	}

	public void setRepo(CrudRepository<T, Serializable> repo) {
		this.repo = repo;
	}

	public void setPersister(Persister<T> persister) {
		this.persister = persister;
	}

	@Override
	public T find(Serializable id) {
		return repo.findOne(id);
	}

	@Override
	public Persister<T> getPersister() {
		return this.persister;
	}

}
