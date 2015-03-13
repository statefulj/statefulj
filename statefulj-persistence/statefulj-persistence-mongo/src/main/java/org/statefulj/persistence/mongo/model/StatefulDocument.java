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
package org.statefulj.persistence.mongo.model;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.statefulj.persistence.annotations.State;

/**
 * A convenience class for a Stateful Document.  This Class ensures read-only behavior for the State
 * field
 * 
 * @author Andrew Hall
 *
 */
public abstract class StatefulDocument {
	
	/**
	 * State field is managed by StatefulJ.
	 * 
	 */
	@State
	@DBRef
	private StateDocument state;
	
	public StateDocument getStateDocument() {
		return state;
	}
}
