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
package org.statefulj.framework.tests.ddd;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.springframework.context.annotation.Scope;
import org.statefulj.framework.core.annotations.FSM;
import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.core.annotations.Transitions;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.fsm.TooBusyException;
import org.statefulj.persistence.jpa.model.StatefulEntity;

import static org.statefulj.framework.tests.ddd.DomainEntity.*;

/**
 * Entity class.  We will scope this as a non-singleton (prototype)
 * 
 * @author Andrew Hall
 *
 */
@Entity
@Scope("prototype") 
@StatefulController(
	clazz=DomainEntity.class,
	startState=STATE_A
)
public class DomainEntity extends StatefulEntity {

	// States
	//
	public final static String STATE_A = "A";
	public final static String STATE_B = "B";
	
	// Internal Events
	//
	private final static String EVENT_X = "event-x";
	private final static String EVENT_Y = "event-y";
	private final static String SPRING_EVENT_X = "springmvc:/" + EVENT_X;
	private final static String SPRING_EVENT_Y = "springmvc:/" + EVENT_Y;
	
	@Id
	private Long id;
	
	private int value;
	
	@FSM
	@Transient
	private StatefulFSM<DomainEntity> fsm;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}
	
	public void onEventX(int value) throws TooBusyException {
		fsm.onEvent(this, EVENT_X, value);
	}

	public void onEventY(int value) throws TooBusyException {
		fsm.onEvent(this, EVENT_Y, value);
	}

	@Transitions({
		@Transition(from=STATE_A, event=EVENT_X, to=STATE_B),
		@Transition(from=STATE_A, event=SPRING_EVENT_X, to=STATE_B),
	})
	protected DomainEntity actionAXB(String event, Integer value) {
		System.out.println("actionAXB");
		this.value = value;
		return this;
	}

	@Transitions({
		@Transition(from=STATE_B, event=EVENT_Y, to=STATE_A),
		@Transition(from=STATE_B, event=SPRING_EVENT_Y, to=STATE_A),
	})
	protected DomainEntity actionBYA(String event, Integer value) {
		System.out.println("actionBYA");
		this.value = value;
		return this;
	}
}
