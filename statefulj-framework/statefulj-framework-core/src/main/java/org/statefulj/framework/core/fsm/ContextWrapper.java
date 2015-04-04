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

/**
 * Wraps the Context of the originating request.  The Context is passed to the 
 * {@link org.statefulj.framework.core.model.Factory} and {@link org.statefulj.framework.core.model.Finder}
 * to create/fetch the Stateful Entity
 * 
 * @author Andrew Hall
 *
 * @param <CT> The Context Type
 */
public class ContextWrapper<CT> {
	
	private CT context;
	
	public ContextWrapper(CT context) {
		this.context = context;
	}

	public CT getContext() {
		return context;
	}
}
