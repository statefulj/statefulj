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
package org.statefulj.framework.core.fsm;

import org.statefulj.fsm.model.Action;
import org.statefulj.fsm.model.State;
import org.statefulj.fsm.model.impl.DeterministicTransitionImpl;

public class TransitionImpl<T> extends DeterministicTransitionImpl<T> {
	
	private boolean any = false;

	public TransitionImpl(
			State<T> from, 
			State<T> to, 
			String event,
			Action<T> action,
			boolean any) {
		super(from, to, event, action);
		this.any = any;
	}

	public boolean isAny() {
		return any;
	}

	public void setAny(boolean any) {
		this.any = any;
	}
}
