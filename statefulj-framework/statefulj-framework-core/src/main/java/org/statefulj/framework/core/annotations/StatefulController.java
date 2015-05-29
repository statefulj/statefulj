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
package org.statefulj.framework.core.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;


/**
 * The StatefulController annotation defines a Finite State Machine
 * for the specified managed Entity.  A StatefulController is a Spring Component that will
 * be managed by Spring via Component scanning.
 * 
 * @author Andrew Hall
 *
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface StatefulController {

	/**
	 * The value may indicate a suggestion for a logical component name,
	 * to be turned into a Spring bean in case of an auto-detected component.
	 * @return the suggested component name, if any
	 */
	String value() default "";
	
	/**
	 * The Starting State.  When the StatefulJ framework is invoked without
	 * a Stateful Entity, then the framework will create a new instance of the 
	 * Stateful Entity via the {@link org.statefulj.framework.core.model.Factory} bean.
	 * The StatefulJ Framework will set the state of the newly created Stateful Entity to 
	 * the startState value.
	 * 
	 * @return Starting State
	 */
	String startState();
	
	/**
	 * The Entity class managed by the StatefulController
	 * 
	 * @return Entity Class
	 */
	Class<?> clazz();

	/**
	 * The name of the managed State field.  If blank, the Entity will be inspected
	 * for a field annotated with State
	 * 
	 * @return Name of the State field
	 */
	String stateField() default "";

	/**
	 * The bean Id of the Factory for this Entity. 
	 * The Factory Class must implement the {@link org.statefulj.framework.core.model.Factory} Interface. 
	 * If not specified, the StatefulJ Framework will use the default Factory Implementation. 
	 * 
	 * @return Id of the Factory Bean
	 */
	String factoryId() default "";
	
	/**
	 * The bean Id of the Finder for this Entity. 
	 * The Finder Class must implement the {@link org.statefulj.framework.core.model.Finder} Interface. 
	 * If not specified, the StatefulJ Framework will use the default Finder Implementation.
	 * 
	 * @return Id of the Finder Bean
	 */
	String finderId() default "";
	
	/**
	 * The bean Id of the Persister for this Entity. 
	 * The Persister is responsible for updating the State field for the Stateful Entity. 
	 * The Persister must implement the {@link org.statefulj.fsm.Persister} Interface. If not specified, the StatefulJ Framework will use the default Persister Implementation.
	 * 
	 * @return Id of the Persister Bean
	 */
	String persisterId() default "";
	
	/**
	 * Defines the set of "Blocking" States.  A Blocking State is a State that "block" an event
	 * from being handled until the FSM transitions out of the Blocking State
	 * 
	 * @return Array of Blocking States
	 */
	String[] blockingStates() default {};

	/**
	 * A set of NOOP {@link org.statefulj.framework.core.annotations.Transition}
	 * 
	 * @return Array of NOOP Transitions
	 */
	Transition[] noops() default {};

	/**
	 * Specify the number of times StatefulJ should attempt to handle the event.  If retryAttempts
	 * is -1, then it will always attempt to handle the event
	 * 
	 * @return number of retry attempts
	 */
	int retryAttempts() default 20;
	
	/**
	 * The interval, in milliseconds, between each retry attempt
	 * 
	 * @return retry interval in milliseconds
	 */
	int retryInterval() default 250;
	
}
