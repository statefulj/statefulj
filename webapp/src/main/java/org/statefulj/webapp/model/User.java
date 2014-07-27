package org.statefulj.webapp.model;

import javax.persistence.Column;
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
    @Column(unique = true, nullable = false)
	Long id;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
