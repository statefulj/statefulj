package org.statefulj.framework.persistence.jpa;

import javax.annotation.Resource;

import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.statefulj.framework.core.model.Factory;
import org.statefulj.framework.core.model.Finder;
import org.statefulj.framework.core.model.FSMHarness;
import org.statefulj.framework.core.model.impl.FSMHarnessImpl;
import org.statefulj.fsm.FSM;
import org.statefulj.fsm.TooBusyException;

public class JPAFSMHarnessImpl<T, CT> implements FSMHarness {
	
	ThreadLocal<TransactionStatus> tl = new ThreadLocal<TransactionStatus>();
	
	@Resource
	JpaTransactionManager transactionManager;
	
	FSMHarness fsm;
	
	public JPAFSMHarnessImpl(
			FSM<T> fsm, 
			Class<T> clazz, 
			Factory<T, CT> factory,
			Finder<T, CT> finder) {
		this.fsm = new FSMHarnessImpl<T, CT>(fsm, clazz, factory, finder);
	}
	
	@Override
	public Object onEvent(final String event, final Object id, final Object[] parms) throws TooBusyException {
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		return tt.execute(new TransactionCallback<Object>() {

			@Override
			public Object doInTransaction(TransactionStatus status) {
				try {
					return fsm.onEvent(event, id, parms);
				} catch (TooBusyException e) {
					throw new RuntimeException(e);
				}
			}
			
		});
	}

	@Override
	public Object onEvent(final String event, final Object[] parms) throws TooBusyException {
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		return tt.execute(new TransactionCallback<Object>() {

			@Override
			public Object doInTransaction(TransactionStatus status) {
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
