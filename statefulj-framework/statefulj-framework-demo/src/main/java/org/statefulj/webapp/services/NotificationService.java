package org.statefulj.webapp.services;

import org.statefulj.webapp.model.Account;
import org.statefulj.webapp.model.Notification;
import org.statefulj.webapp.model.User;

public interface NotificationService  {
	
	void onNotification(User user, Account account, String msg);
	
	void save(Notification notification);
	
}
