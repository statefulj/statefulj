package org.statefulj.webapp.services;

import javax.servlet.http.HttpSession;

import org.statefulj.webapp.model.User;

public interface UserService  {
	
	User findById(Long id);
	
	User findByEmail(String email);
	
	User findLoggedInUser();
	
	void save(User user);
	
	void login(HttpSession session, User user);

}
