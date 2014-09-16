package org.statefulj.persistence.common;

import org.statefulj.persistence.common.annotations.State;

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
