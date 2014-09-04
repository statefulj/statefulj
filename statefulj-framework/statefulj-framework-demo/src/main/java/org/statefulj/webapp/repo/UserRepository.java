package org.statefulj.webapp.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.statefulj.webapp.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
	
	User findByEmail(String email);
 
}
