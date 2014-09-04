package org.statefulj.webapp.services;

import javax.servlet.http.HttpSession;

import org.statefulj.webapp.model.User;

public interface UserSessionService  {
	
	User findLoggedInUser();

	void login(HttpSession session, User user);

}
