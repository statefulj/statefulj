package org.statefulj.framework.dao;

import org.springframework.data.repository.Repository;
import org.statefulj.framework.model.User;

public interface UserRepository extends Repository<User, Long> {

	User save(User User);
	
	User findOne(Long id);

}
