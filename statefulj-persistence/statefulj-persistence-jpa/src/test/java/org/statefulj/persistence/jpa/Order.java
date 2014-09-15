package org.statefulj.persistence.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.statefulj.persistence.jpa.model.StatefulEntity;

@Entity
@Table(name="Orders")
public class Order extends StatefulEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(unique = true, nullable = false)
	private long id;

	private int amount;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}
	
	
}
