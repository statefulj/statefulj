/***
 * 
 * Copyright 2014 Andrew Hall
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.statefulj.persistence.jpa;

import java.lang.reflect.Field;
import java.util.List;

import javax.persistence.EmbeddedId;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionCallback;
import org.statefulj.fsm.Persister;
import org.statefulj.fsm.StaleStateException;
import org.statefulj.fsm.model.State;
import org.statefulj.persistence.common.AbstractPersister;

import static org.statefulj.common.utils.ReflectionUtils.*;

public class JPAPerister<T> extends AbstractPersister<T> implements Persister<T> {

	private static final Logger logger = LoggerFactory.getLogger(JPAPerister.class);

	private EntityManager entityManager;
  	
	private PlatformTransactionManager transactionManager;

	public JPAPerister(List<State<T>> states, State<T> start, Class<T> clazz, EntityManagerFactoryInfo entityManagerFactory, PlatformTransactionManager transactionManager) {
		this(states, null, start, clazz, entityManagerFactory.getNativeEntityManagerFactory().createEntityManager(), transactionManager);
	}

	public JPAPerister(List<State<T>> states, String stateFieldName, State<T> start, Class<T> clazz, EntityManager entityManager, PlatformTransactionManager transactionManager) {
		super(states, stateFieldName, start, clazz);
		this.transactionManager = transactionManager;
		this.entityManager = entityManager;
	}

	/**
	 * Set the current State.  This method will ensure that the state in the db matches the expected current state.  
	 * If not, it will throw a StateStateException
	 * 
	 * @param stateful Stateful Entity
	 * @param current Expected current State
	 * @param next The value of the next State
	 * @throws StaleStateException thrown if the value of the State does not equal to the provided current State
	 */
	@Override
	public void setCurrent(T stateful, State<T> current, State<T> next) throws StaleStateException {
		try {
			
			// Has this Entity been persisted to the database? 
			//
			Object id = getId(stateful);
			if (id != null && entityManager.contains(stateful)) {
				updateStateInDB(stateful, current, next, id);
				setState(stateful, next.getName());
			} else {
				updateStateInMemory(stateful, current, next);
			}
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param stateful
	 * @param current
	 * @param next
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 * @throws StaleStateException
	 */
	private void updateStateInMemory(T stateful, State<T> current, State<T> next)
			throws NoSuchFieldException, IllegalAccessException,
			StaleStateException {
		// The Entity hasn't been persisted to the database - so it exists only
		// this Application memory.  So, serialize the qualified update to prevent
		// concurrency conflicts
		//
		synchronized(stateful) {
			String state = this.getState(stateful);
			state = (state == null) ? getStart().getName() : state;
			if (state.equals(current.getName())) {
				setState(stateful, next.getName());
			} else {
				throwStaleState(current, next);
			}
		}
	}

	/**
	 * @param stateful
	 * @param current
	 * @param next
	 * @param id
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 * @throws StaleStateException
	 */
	private void updateStateInDB(T stateful, State<T> current, State<T> next,
			Object id) throws NoSuchFieldException, IllegalAccessException,
			StaleStateException {
		// Entity is in the database - perform qualified update based off 
		// the current State value
		//
		Query update = buildUpdate(id, stateful, current, next, getIdField(), getStateField());
		
		// Successful update?
		//
		if (update.executeUpdate() == 0) {
			
			// If we aren't able to update - it's most likely that we are out of sync.
			// So, fetch the latest value and update the Stateful object.  Then throw a RetryException
			// This will cause the event to be reprocessed by the FSM
			//
			final Query query = buildQuery(id, stateful);
			String state = getStart().getName();
			try {
				TransactionTemplate tt = new TransactionTemplate(transactionManager);
				state =  tt.execute(new TransactionCallback<String>() {

					@Override
					public String doInTransaction(TransactionStatus status) {
						return (String) query.getSingleResult();
					}
					
				});
			} catch(NoResultException nre) {
				// This is the first time setting the state, ignore
				//
			}
			
			logger.warn("Stale State, expected={}, actual={}", current.getName(), state);
			
			setState(stateful, state);
			throwStaleState(current, next);
		}
	}
	
	protected Query buildUpdate(
			Object id, 
			T stateful, 
			State<T> current, 
			State<T> next, 
			Field idField, 
			Field stateField) throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {

		CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
		
		// update <class>
		//
		CriteriaUpdate<T> cu = cb.createCriteriaUpdate(this.getClazz());
		Root<T> t = cu.from(this.getClazz());
		
		Path<?> idPath = t.get(this.getIdField().getName());
		Path<String> statePath = t.get(this.getStateField().getName());
		
		// set state=<next_state>
		//
		cu.set(statePath, next.getName());
		
		// where id=<id> and state=<current_state>
		//
		Predicate statePredicate = (current.equals(getStart())) ?
				cb.or(
					cb.equal(
						statePath, 
						current.getName()
					),
					cb.equal(
						statePath, 
						cb.nullLiteral(String.class)
					)
				) :
				cb.equal(
					statePath, 
					current.getName()
				);
				
		cu.where(
			cb.and(
				cb.equal(
					idPath, 
					this.getId(stateful)
				),
				statePredicate
			)
		);
		
		Query query = entityManager.createQuery(cu);
		if (logger.isDebugEnabled()) {
			logger.debug(query.unwrap(org.hibernate.Query.class).getQueryString());
		}
		return query;
	}

	@Override
	protected boolean validStateField(Field stateField) {
		return (stateField.getType().equals(String.class));
	}

	@Override
	protected Field findIdField(Class<?> clazz) {
		Field idField = null;
		idField = getReferencedField(clazz, Id.class);
		if (idField == null) {
			idField = getReferencedField(clazz, EmbeddedId.class);
		}
		return idField;
	}

	@Override
	protected Class<?> getStateFieldType() {
		return String.class;
	}

	private Query buildQuery(Object id, T stateful) throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
		
		CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
		CriteriaQuery<String> cq = cb.createQuery(String.class);
		
		Root<T> t = cq.from(this.getClazz());
		Path<?> idPath = t.get(this.getIdField().getName());
		Path<String> statePath = t.get(this.getStateField().getName());
		
		cq.select(statePath);
		
		cq.where(cb.equal(idPath, this.getId(stateful)));

		Query query = entityManager.createQuery(cq);
		if (logger.isDebugEnabled()) {
			logger.debug(query.unwrap(org.hibernate.Query.class).getQueryString());
		}
		return query;
	}
}
