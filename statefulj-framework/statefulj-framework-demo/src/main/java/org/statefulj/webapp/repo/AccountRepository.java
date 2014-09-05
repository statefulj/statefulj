package org.statefulj.webapp.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.statefulj.webapp.model.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {
	
}
