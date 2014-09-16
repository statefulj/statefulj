
package org.statefulj.persistence.jpa;

import java.lang.reflect.Field;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.springframework.transaction.annotation.Transactional;
import org.statefulj.common.utils.ReflectionUtils;
import org.statefulj.fsm.Persister;
import org.statefulj.fsm.StaleStateException;
import org.statefulj.fsm.model.State;
import org.statefulj.persistence.common.AbstractPersister;

// TODO : Rewrite this to use "safe" query building instead of string construction
//
@Transactional
public class JPAPerister<T> extends AbstractPersister<T> implements Persister<T> {

	@PersistenceContext
	private EntityManager entityManager;
	
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
				String update = buildUpdateStatement(id, stateful, current, next, getIdField(), getStateField());
				
				// Successful update?
				//
				if (entityManager.createQuery(update).executeUpdate() == 0) {
					
					// If we aren't able to update - it's most likely that we are out of sync.
					// So, fetch the latest value and update the Stateful object.  Then throw a RetryException
					// This will cause the event to be reprocessed by the FSM
					//
					String query = String.format(
							"select %s from %s where %s=%s", 
							this.getStateField().getName(), 
							getClazz().getSimpleName(),
							this.getIdField().getName(),
							id);
					String state = getStart().getName();
					try {
						state = (String)entityManager.createQuery(query).getSingleResult();
					} catch(NoResultException nre) {
						// This is the first time setting the state, ignore
						//
					}
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
	
	protected String buildUpdateStatement(
			Object id, 
			T stateful, 
			State<T> current, 
			State<T> next, 
			Field idField, 
			Field stateField) {
		
		String where = (current.equals(getStart())) 
				?
				String.format(
						"%s=%s and (%s='%s' or %s is null)",
						getIdField().getName(),
						id,
						getStateField().getName(), 
						current.getName(),
						getStateField().getName()) 
				:
				String.format(
						"%s=%s and %s='%s'",
						getIdField().getName(),
						id,
						getStateField().getName(), 
						current.getName());
		
		String update = String.format(
				"update %s set %s='%s' where %s", 
				getClazz().getSimpleName(), 
				getStateField().getName(), 
				next.getName(),
				where);
		
		return update;
	}

	@Override
	protected boolean validStateField(Field stateField) {
		return (stateField.getType().equals(String.class));
	}

	@Override
	protected Field findIdField(Class<?> clazz) {
		return ReflectionUtils.getAnnotatedField(clazz, Id.class);
	}

	@Override
	protected Class<?> getStateFieldType() {
		return String.class;
	}
	
}
