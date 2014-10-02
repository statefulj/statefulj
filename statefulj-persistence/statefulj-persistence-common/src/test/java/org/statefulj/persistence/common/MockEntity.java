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
package org.statefulj.persistence.common;

import org.statefulj.persistence.annotations.State;

public class MockEntity {
	
	String idField;
	
	String stateField1;

	@State
	String stateField2;

	public String getIdField() {
		return idField;
	}

	public void setIdField(String idField) {
		this.idField = idField;
	}

	public String getStateField1() {
		return stateField1;
	}

	public void setStateField1(String stateFielda) {
		this.stateField1 = stateFielda;
	}

	public String getStateField2() {
		return stateField2;
	}

	public void setStateField2(String stateField2) {
		this.stateField2 = stateField2;
	}
}
