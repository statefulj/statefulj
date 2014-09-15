package org.statefulj.framework.core.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;


// TODO : Add in Blocking support
// TODO : Add in multiple State field support
// TODO : Add in explicit Mongo or jpa persistence
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
/**
 * The StatefuleController annotation defines a Finite State Machine
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
	 * Optional Ids of the Persistence beans for this class. 
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
