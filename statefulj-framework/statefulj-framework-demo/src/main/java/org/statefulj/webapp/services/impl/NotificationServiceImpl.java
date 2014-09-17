package org.statefulj.webapp.services.impl;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Service;
import org.statefulj.framework.core.annotations.FSM;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.fsm.TooBusyException;

import static org.statefulj.webapp.controller.NotificationController.*;

import org.statefulj.webapp.model.Account;
import org.statefulj.webapp.model.Notification;
import org.statefulj.webapp.model.User;
import org.statefulj.webapp.repo.NotificationRepository;
import org.statefulj.webapp.services.NotificationService;

@Service("notificationService")
public class NotificationServiceImpl implements NotificationService {

	@FSM
	StatefulFSM<Notification> notificationFSM;
	
	@Resource
	NotificationRepository notificationRepo;

	@PersistenceContext
	EntityManager entityManager;
	
	@Override
	public void notify(User user, Account account, String msg) {
		try {
			notificationFSM.onEvent(NOTIFY, user, account, msg);
		} catch (TooBusyException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void save(Notification notification) {
		notificationRepo.save(notification);
		entityManager.flush();
	}
}
