package org.statefulj.persistence.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.statefulj.persistence.mongo.model.StatefulDocument;

@Document
public class Order extends StatefulDocument {

	@Id
	private String id;

	private int amount;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}
}
