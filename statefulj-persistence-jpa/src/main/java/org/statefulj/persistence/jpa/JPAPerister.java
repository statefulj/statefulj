package org.statefulj.persistence.jpa;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.springframework.transaction.annotation.Transactional;
import org.statefulj.fsm.Persister;
import org.statefulj.fsm.StaleStateException;
import org.statefulj.fsm.model.State;

// TODO : Rewrite this to use "safe" queury building instead of string construction
//
@Transactional
public class JPAPerister<T> implements Persister<T> {

	@PersistenceContext
	private EntityManager entityManager;
	
	private String clazz;
	private Field idField;
	private Field stateField;
	private State<T> start;
	private HashMap<String, State<T>> states = new HashMap<String, State<T>>();
	
	public JPAPerister(List<State<T>> states, State<T> start, Class<T> clazz) {
		
		// Find the Id and State<T> field of the Entity
		//
		this.clazz = clazz.getSimpleName();
		this.idField = getAnnotatedField(clazz, Id.class);
		
		if (this.idField == null) {
			throw new RuntimeException("No Id field defined");
		}
		this.idField.setAccessible(true);
		
		this.stateField = getAnnotatedField(clazz, org.statefulj.persistence.jpa.annotations.State.class);
		if (this.stateField == null) {
			throw new RuntimeException("No State field defined");
		}
		if (!this.stateField.getType().equals(String.class)) {
			throw new RuntimeException(
					String.format(
							"State field, %s, of class %s, is not of type String",
							this.stateField.getName(),
							clazz));
		}
		this.stateField.setAccessible(true);

		// Start state - returned when no state is set
		//
		this.start = start;
		
		// Index States into a HashMap
		//
		for(State<T> state : states) {
			this.states.put(state.getName(), state);
		}
		
	}

	/**
	 * Return the current State 
	 */
	public State<T> getCurrent(T stateful) {
		State<T> state = null;
		try {
			String stateKey = this.getState(stateful);
			state = (stateKey == null ) ? this.start : this.states.get(stateKey);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		state = (state == null) ? this.start : state;
		return state;
	}

	/**
	 * Overwrite the value in the db and in the stateful Entity
	 * 
	 * @param stateful
	 * @param current
	 */
	public void setCurrent(T stateful, State<T> current) {
		try {
			Number id = getId(stateful);
			String update = String.format(
					"update %s set %s='%s' where %s=%s", 
					this.clazz, 
					this.stateField.getName(), 
					current.getName(),
					this.idField.getName(),
					id);
			if (entityManager.createQuery(update).executeUpdate() == 0) {
				throw new RuntimeException("Unable to set state");
			}
			setState(stateful, current.getName());
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
			Number id = getId(stateful);
			if (id != null && entityManager.contains(stateful)) {
				String where = (current.equals(this.start)) 
						?
						String.format(
								"%s=%s and (%s='%s' or %s is null)",
								this.idField.getName(),
								id,
								this.stateField.getName(), 
								current.getName(),
								this.stateField.getName()) 
						:
						String.format(
								"%s=%s and %s='%s'",
								this.idField.getName(),
								id,
								this.stateField.getName(), 
								current.getName());
				
				String update = String.format(
						"update %s set %s='%s' where %s", 
						this.clazz, 
						this.stateField.getName(), 
						next.getName(),
						where);
				
				// Successful update?
				//
				if (entityManager.createQuery(update).executeUpdate() == 0) {
					
					// If we aren't able to update - it's most likely that we are out of sync.
					// So, fetch the latest value and update the stateful object.  Then throw a RetryException
					// This will cause the event to be reprocessed by the FSM
					//
					String query = String.format(
							"select %s from %s where %s=%s", 
							this.stateField.getName(), 
							this.clazz,
							this.idField.getName(),
							id);
					String state = this.start.getName();
					try {
						state = (String)entityManager.createQuery(query).getSingleResult();
					} catch(NoResultException nre) {
						// If it hasn't been 
					}
					setState(stateful, state);
					throwStaleState(current, next);
				}
				setState(stateful, next.getName());
			} else {
				synchronized(stateful) {
					String state = this.getState(stateful);
					state = (state == null) ? this.start.getName() : state;
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
	
	/**
	 * Return a field by an Annotation
	 * 
	 * @param clazz
	 * @param annotationClass
	 * @return
	 */
	private Field getAnnotatedField(
			Class<?> clazz,
			Class<? extends Annotation> annotationClass) {
		Field match = null;
		if (clazz != null) {
			match = getAnnotatedField(clazz.getSuperclass(), annotationClass);
			if (match == null) {
				for(Field field : clazz.getDeclaredFields()) {
					if (field.isAnnotationPresent(annotationClass)) {
						match = field;
						break;
					}
				}
				
			}
		}
		
		return match;
	}
	
	private Number getId(T obj) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		return (Number)this.idField.get(obj);
	}
	
	private String getState(T obj) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		return (String)this.stateField.get(obj);
	}
	
	private void setState(T obj, String state) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		state = (state == null) ? this.start.getName() : state;
		this.stateField.set(obj, state);
	}

	private void throwStaleState(State<T> current, State<T> next) throws StaleStateException {
		String err = String.format(
				"Unable to update state, entity.state=%s, db.state=%s",
				current.getName(),
				next.getName());
		throw new StaleStateException(err);
	}
}
