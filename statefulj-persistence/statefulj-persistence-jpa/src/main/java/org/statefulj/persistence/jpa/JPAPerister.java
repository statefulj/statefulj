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

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.statefulj.fsm.Persister;
import org.statefulj.fsm.StaleStateException;
import org.statefulj.fsm.model.State;
import org.statefulj.persistence.common.AbstractPersister;

import static org.statefulj.common.utils.ReflectionUtils.*;

// TODO : Rewrite this to use "safe" query building instead of string construction
//
@Transactional
public class JPAPerister<T> extends AbstractPersister<T> implements Persister<T> {

	Logger logger = LoggerFactory.getLogger(JPAPerister.class);

	@PersistenceContext
	private EntityManager entityManager;
	
	public JPAPerister(List<State<T>> states, State<T> start, Class<T> clazz) {
		this(states, null, start, clazz);
	}

	public JPAPerister(List<State<T>> states, String stateFieldName, State<T> start, Class<T> clazz) {
		super(states, stateFieldName, start, clazz);
	}

	/**
	 * Set the current State.  This method will ensure that the state in the db matches the expected current state.  
	 * If not, it will throw a StateStateException
	 * 
	 * @param stateful
	 * @param current
	 * @param next
	 * @throws StaleStateException 
	 */
	public void setCurrent(T stateful, State<T> current, State<T> next) throws StaleStateException {
		try {
			
			// Has this Entity been persisted to the database? 
			//
			Object id = getId(stateful);
			if (id != null && entityManager.contains(stateful)) {
				
				// Entity is in the database - perform qualified update based off 
				// the current State value
				//
				Query update = buildUpdateStatement(id, stateful, current, next, getIdField(), getStateField());
				
				// Successful update?
				//
				if (update.executeUpdate() == 0) {
					
					// If we aren't able to update - it's most likely that we are out of sync.
					// So, fetch the latest value and update the Stateful object.  Then throw a RetryException
					// This will cause the event to be reprocessed by the FSM
					//
					Query query = buildQuery(id, stateful);
					String state = getStart().getName();
					try {
						state = (String)query.getSingleResult();
					} catch(NoResultException nre) {
						// This is the first time setting the state, ignore
						//
					}
					
					logger.warn("Stale State, expected={}, actual={}", current.getName(), state);
					
					setState(stateful, state);
					throwStaleState(current, next);
				}
				setState(stateful, next.getName());
			} else {
				
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
	
	protected Query buildUpdateStatement(
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
		
		// set state=<new_state>
		//
		cu.set(statePath, next.getName());
		
		// where id=<id> and state=<old_state>
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
		logger.debug(query.unwrap(org.hibernate.Query.class).getQueryString());
		return query;
	}

	@Override
	protected boolean validStateField(Field stateField) {
		return (stateField.getType().equals(String.class));
	}

	@Override
	protected Field findIdField(Class<?> clazz) {
		return getReferencedField(clazz, Id.class);
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
		logger.debug(query.unwrap(org.hibernate.Query.class).getQueryString());
		return query;
	}
}
