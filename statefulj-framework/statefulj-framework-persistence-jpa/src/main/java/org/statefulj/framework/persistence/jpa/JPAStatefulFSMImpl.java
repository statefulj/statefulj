package org.statefulj.framework.persistence.jpa;

import javax.annotation.Resource;

import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.statefulj.framework.core.model.Factory;
import org.statefulj.framework.core.model.Finder;
import org.statefulj.framework.core.model.impl.StatefulFSMImpl;
import org.statefulj.fsm.FSM;
import org.statefulj.fsm.TooBusyException;

public class JPAStatefulFSMImpl<T> extends StatefulFSMImpl<T> {
	
	ThreadLocal<TransactionStatus> tl = new ThreadLocal<TransactionStatus>();
	
	@Resource
	JpaTransactionManager transactionManager;
	
	public JPAStatefulFSMImpl(
			FSM<T> fsm, 
			Class<T> clazz, 
			Factory<T> factory,
			Finder<T> finder) {
		super(fsm, clazz, factory, finder);
	}
	
	@Override
	public T onEvent(String event, Object id, Object[] parms) throws TooBusyException {
		startTransaction(transactionManager);
		try {
			T val = super.onEvent(event, id, parms);
			commitTransaction(transactionManager);
			return val;
		} catch(Throwable t) {
			rollbackTransaction(transactionManager);
			throw t;
		} 
	}

	@Override
	public T onEvent(String event, Object[] parms) throws TooBusyException {
		startTransaction(transactionManager);
		try {
			T val = super.onEvent(event, parms);
			commitTransaction(transactionManager);
			return val;
		} catch(Throwable t) {
			rollbackTransaction(transactionManager);
			throw t;
		}
	}
	
	public void startTransaction(JpaTransactionManager transactionManager) {
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setName(Thread.currentThread().getName() + ".tx");
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		TransactionStatus status = transactionManager.getTransaction(def);
		tl.set(status);
	}

	public void commitTransaction(JpaTransactionManager transactionManager) {
		TransactionStatus ts = tl.get();
		if (!ts.isCompleted()) {
			transactionManager.commit(ts);
		}
	}

	public void rollbackTransaction(JpaTransactionManager transactionManager) {
		TransactionStatus ts = tl.get();
		if (!ts.isCompleted()) {
			transactionManager.rollback(ts);
		}
	}
}
