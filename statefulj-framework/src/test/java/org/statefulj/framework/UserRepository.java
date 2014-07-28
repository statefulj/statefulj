package org.statefulj.framework;

import org.springframework.data.repository.Repository;

public interface UserRepository extends Repository<User, Long> {

	User save(User User);
	
	User findOne(Long id);

}
