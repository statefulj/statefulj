package org.statefulj.framework.persistence.jpa;

import javax.annotation.Resource;

import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.statefulj.framework.core.model.Factory;
import org.statefulj.framework.core.model.Finder;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.framework.core.model.impl.StatefulFSMImpl;
import org.statefulj.fsm.FSM;
import org.statefulj.fsm.TooBusyException;

public class JPAStatefulFSMImpl<T> implements StatefulFSM<T> {
	
	ThreadLocal<TransactionStatus> tl = new ThreadLocal<TransactionStatus>();
	
	@Resource
	JpaTransactionManager transactionManager;
	
	StatefulFSM<T> fsm;
	
	public JPAStatefulFSMImpl(
			FSM<T> fsm, 
			Class<T> clazz, 
			Factory<T> factory,
			Finder<T> finder) {
		this.fsm = new StatefulFSMImpl<T>(fsm, clazz, factory, finder);
	}
	
	@Override
	public T onEvent(final String event, final Object id, final Object[] parms) throws TooBusyException {
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		return tt.execute(new TransactionCallback<T>() {

			@Override
			public T doInTransaction(TransactionStatus status) {
				try {
					return fsm.onEvent(event, id, parms);
				} catch (TooBusyException e) {
					throw new RuntimeException(e);
				}
			}
			
		});
	}

	@Override
	public T onEvent(final String event, final Object[] parms) throws TooBusyException {
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		return tt.execute(new TransactionCallback<T>() {

			@Override
			public T doInTransaction(TransactionStatus status) {
				try {
					return fsm.onEvent(event, parms);
				} catch (TooBusyException e) {
					throw new RuntimeException(e);
				} catch (InstantiationException e) {
					throw new RuntimeException(e);
				}
			}
			
		});
	}
}
