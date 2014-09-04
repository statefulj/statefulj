package org.statefulj.webapp.services;

import org.statefulj.webapp.model.User;

public interface UserService  {
	
	User findById(Long id);
	
	User findByEmail(String email);
	
	void save(User user);
	
}
