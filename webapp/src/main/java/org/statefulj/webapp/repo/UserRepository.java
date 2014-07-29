package org.statefulj.webapp.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;
import org.statefulj.webapp.model.User;

public interface UserRepository extends CrudRepository<User, Long> {
 
}
