package org.statefulj.framework.core.dao;

import org.springframework.data.repository.CrudRepository;
import org.statefulj.framework.core.model.User;

public interface UserRepository extends CrudRepository<User, Long> {

}
