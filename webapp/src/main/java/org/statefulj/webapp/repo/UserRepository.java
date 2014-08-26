package org.statefulj.webapp.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.statefulj.webapp.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
 
}
