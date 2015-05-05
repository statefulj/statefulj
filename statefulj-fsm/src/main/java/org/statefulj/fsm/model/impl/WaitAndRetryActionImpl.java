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
package org.statefulj.fsm.model.impl;

import org.statefulj.fsm.RetryException;
import org.statefulj.fsm.WaitAndRetryException;
import org.statefulj.fsm.model.Action;

/**
 * Action with throws a {@link org.statefulj.fsm.WaitAndRetryException}
 * 
 * @author Andrew Hall
 *
 * @param <T>
 */
public class WaitAndRetryActionImpl<T> implements Action<T> {
	
	private int wait = 0;

	/**
	 * Constructor with a wait time expressed in milliseconds
	 * 
	 * @param wait time in milliseconds
	 */
	public WaitAndRetryActionImpl(int wait) {
		this.wait = wait;
	}
	
	@Override
	public void execute(T obj, String event, Object... args) throws RetryException {
		throw new WaitAndRetryException(this.wait);
	}

}
