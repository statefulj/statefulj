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
package org.statefulj.framework.core.model;

import org.statefulj.framework.core.annotations.FSM;
import org.statefulj.fsm.TooBusyException;

/**
 * The StatefulFSM provides the ability to route Events directly to the FSM. 
 * This is done by invoking the onEvent method of the associated StatefulFSM class. 
 * To obtain a reference to the StatefulFSM, you annotate a reference with the {@link FSM} annotation. 
 * The StatefulJ Framework will automatically inject the StatefulFSM based off the declared Generic Type. 
 * If there is more than one {@link org.statefulj.framework.core.annotations.StatefulController} that qualifies, then you can provide the bean Id of 
 * the specific {@link org.statefulj.framework.core.annotations.StatefulController}
 * 
 * @author Andrew Hall
 *
 * @param <T> The class of the Stateful Entity
 */
public interface StatefulFSM<T> {
	
	/**
	 * Pass an event to the FSM for a non-existent Stateful Entity.  The StatefulJ framework will instiate a 
	 * new Stateful Event by invoking the {@link Factory}
	 * 
	 * @param event the Event
	 * @param parms Optional parameters passed into the Action method
	 * @return the returned Object from the Action Method
	 * @throws TooBusyException thrown if the FSM cannot process the event
	 */
	Object onEvent(String event, Object... parms)  throws TooBusyException ;

	/**
	 * Pass an event to the FSM for existing Stateful Entity
	 * 
	 * @param stateful the Stateful Entity
	 * @param event the Event
	 * @param parms Optional parameters passed into the Action method
	 * @return the returned Object from the Action Method
	 * @throws TooBusyException thrown if the FSM cannot process the event
	 */
	Object onEvent(T stateful, String event, Object... parms)  throws TooBusyException ;

}
