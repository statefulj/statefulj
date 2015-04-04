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

import org.springframework.beans.factory.annotation.Autowired;

/**
 * The FSM annotation denotes an injection of the {@link org.statefulj.framework.core.model.StatefulFSM}.  
 * The StatefulJ framework will determine which StatefulFSM to inject based off the generic type of the StatefulFSM.
 * To disambiguate between multiple StatefulFSM for a type, provide the bean Id of the Stateful Controller 
 * using the {@link org.statefulj.framework.core.annotations.FSM#value()} parameter
 * 
 * @author Andrew Hall
 *
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Autowired
public @interface FSM {
	
	/**
	 * The Id of the Stateful Controller. If not specified, it will determine the FSM based off the Generic Type
	 * 
	 * @return
	 */
	public String value() default "";
	
}
