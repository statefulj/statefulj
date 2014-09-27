/***
 * 
 * Copyright 2014 Andrew Hall
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
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
