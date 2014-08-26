package org.statefulj.framework.tests.dao;

import org.springframework.data.repository.CrudRepository;
import org.statefulj.framework.tests.model.User;

public interface UserRepository extends CrudRepository<User, Long> {

}
