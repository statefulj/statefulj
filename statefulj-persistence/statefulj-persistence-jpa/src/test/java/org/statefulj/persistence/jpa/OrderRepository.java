package org.statefulj.persistence.jpa;

import org.springframework.data.repository.Repository;

public interface OrderRepository extends Repository<Order, Long> {

	Order save(Order order);
	
	Order findOne(Long id);

}
