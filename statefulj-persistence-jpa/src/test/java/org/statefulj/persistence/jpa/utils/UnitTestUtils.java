package org.statefulj.persistence.jpa.utils;

import javax.persistence.EntityManager;

import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

public class UnitTestUtils {
	
	static ThreadLocal<TransactionStatus> tl = new ThreadLocal<TransactionStatus>();
	
	public static void startTransaction(JpaTransactionManager transactionManager) {
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setName("SomeTxName");
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		TransactionStatus status = transactionManager.getTransaction(def);
		tl.set(status);
	}

	public static void commitTransaction(JpaTransactionManager transactionManager) {
		transactionManager.commit(tl.get());
	}

	public static void rollbackTransaction(JpaTransactionManager transactionManager) {
		transactionManager.rollback(tl.get());
	}

}
