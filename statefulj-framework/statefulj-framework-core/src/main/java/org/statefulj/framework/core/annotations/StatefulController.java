package org.statefulj.framework.core.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
/**
 * The StatefuleController annotation denotes a Class that defines a Finite State Machine
 * for the specified managed Entity.  A StatefulController is a Spring Component that will
 * be managed by Spring via Component scanning.
 * 
 * @author Andrew Hall
 *
 */
public @interface StatefulController {

	/**
	 * The value may indicate a suggestion for a logical component name,
	 * to be turned into a Spring bean in case of an autodetected component.
	 * @return the suggested component name, if any
	 */
	String value() default "";
	
	/**
	 * The Starting State.  If there is a transition from this State, the framework
	 * will pass in a new instance of the Managed Entity.  It is the responsibility of
	 * the StatefulController to persist the new instance.
	 * 
	 * @return
	 */
	String startState();
	
	/**
	 * The Entity class managed by the StatefulController
	 * 
	 * @return
	 */
	Class<?> clazz();

	/**
	 * Optional Id of the PersistenceSupport bean for this class.  If not specified, a 
	 * PersistenceSupport bean is dynamically generated based on the 
	 * type of Repository supporting the managed Entity.  The PersistenceSupport Bean is 
	 * responsible for fetching the Entity from the Database, as well as, updating the 
	 * State of the Managed Entity for each Transition.
	 * 
	 * @return
	 */
	String factoryId() default "";
	String finderId() default "";
	String persisterId() default "";

	/**
	 * A set of NOOP transitions
	 * 
	 * @return
	 */
	Transition[] noops() default {};

}
