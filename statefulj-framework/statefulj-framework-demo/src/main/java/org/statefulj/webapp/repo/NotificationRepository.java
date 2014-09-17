package org.statefulj.webapp.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.statefulj.webapp.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
	
 
}
