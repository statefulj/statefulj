package org.statefulj.webapp.controller;

import javax.annotation.Resource;

import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.webapp.model.Account;
import org.statefulj.webapp.model.Notification;
import org.statefulj.webapp.model.User;

import static org.statefulj.webapp.model.Notification.*;

import org.statefulj.webapp.services.NotificationService;

@StatefulController(
	clazz=Notification.class,
	startState=NON_EXISTENT
)
public class NotificationController {
	
	// EVENTS
	//
	public static final String NOTIFY = "notify";
	
	@Resource
	NotificationService notificationService;
	
	@Transition(from=NON_EXISTENT, event=NOTIFY, to=SHOWING)
	public void createNotification(Notification notification, String event, User user, Account account, String msg) {
		notification.setType(account.getState());
		notification.setMessage(msg);
		user.addNotification(notification);
	}
}
