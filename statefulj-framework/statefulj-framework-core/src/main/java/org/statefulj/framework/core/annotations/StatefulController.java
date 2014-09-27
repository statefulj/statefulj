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


// TODO : Add in Blocking support
// TODO : Add in explicit Mongo or jpa persistence
// TODO : Add in licensing
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
	 * The name of the managed State field.  If blank, the Entity will be inspected
	 * for a field annotated with State
	 * 
	 * @return
	 */
	String stateField() default "";

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
