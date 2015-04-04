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
 * A Transition is a reaction to an Event directed at a Stateful Entity. 
 * The Transition can involve a possible change in State and a possible Action.
 * If you want to map multiple Transitions to a method, encapsulate the Transition annotation
 * with a {@link org.statefulj.framework.core.annotations.Transitions} annotation.  If you
 * want to change State without invoking a method, include the Transition within the {@link org.statefulj.framework.core.annotations.StatefulController#noops()}
 * field
 * 
 * @author Andrew Hall
 *
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Transition {
	
	final String ANY_STATE = "*";
	
	/**
	 * Defines the "from" State.  A value of "*" implies that any State qualifies as a "from" State
	 * @return the "from" State value
	 */
	String from() default ANY_STATE;
	
	/**
	 * Defines the Event
	 * 
	 * @return the event
	 */
	String event();

	/**
	 * Defines to the "to" State.  A value of "*" implies that the "to" state is the value of the current State
	 * 
	 * @return the "to" State value
	 */
	String to() default ANY_STATE;
	
	/**
	 * Defines whether to reload the Stateful Entity before invoking the method.  Set to true to manage
	 * potential concurrency conflicts
	 * 
	 * @return If true, will reload the Stateful Entity before invoking the method
	 */
	boolean reload() default false;
}
