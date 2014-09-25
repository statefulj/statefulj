package org.statefulj.persistence.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository("orderRepository")
public interface OrderRepository extends MongoRepository<Order, String> {

}
