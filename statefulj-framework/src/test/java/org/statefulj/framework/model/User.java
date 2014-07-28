package org.statefulj.framework.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.statefulj.persistence.jpa.model.StatefulEntity;


@Entity
@Table(name="users")
public class User extends StatefulEntity {
	
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	Long id;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
