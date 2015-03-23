package org.statefulj.persistence.mongo;

import org.springframework.data.mongodb.core.MongoTemplate;

public class MongoUtils {

	public static void dropDB(MongoTemplate mongoTemplate) {
		mongoTemplate.getDb().dropDatabase();
	}
}
