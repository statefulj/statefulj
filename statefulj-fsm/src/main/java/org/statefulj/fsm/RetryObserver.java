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
package org.statefulj.fsm;

/**
 * RetryObserver is invoke on each retry.  It is responsible for fetching a fresh
 * version of the Stateful Entity.
 * 
 * @author Andrew Hall
 *
 */
public interface RetryObserver<T> {

	/**
	 * Fetch a new version of the Stateful Entity
	 * @param stateful
	 * @return a new version of the Stateful Entity
	 */
	T onRetry(T stateful, String event, Object... args);
}
