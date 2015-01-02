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
package org.statefulj.framework.persistence.jpa;

import javax.annotation.Resource;

import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.statefulj.framework.core.model.Factory;
import org.statefulj.framework.core.model.Finder;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.framework.core.model.impl.FSMHarnessImpl;
import org.statefulj.fsm.TooBusyException;

public class JPAFSMHarnessImpl<T, CT> extends FSMHarnessImpl<T, CT> {
	
	@Resource
	JpaTransactionManager transactionManager;
	
	public JPAFSMHarnessImpl(
			StatefulFSM<T> fsm, 
			Class<T> clazz, 
			Factory<T, CT> factory,
			Finder<T, CT> finder) {
		super(fsm, clazz, factory, finder);
	}
	
	@Override
	public Object onEvent(final String event, final Object id, final Object[] parms) throws TooBusyException {
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		return tt.execute(new TransactionCallback<Object>() {

			@Override
			public Object doInTransaction(TransactionStatus status) {
				try {
					return JPAFSMHarnessImpl.super.onEvent(event, id, parms);
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
					return JPAFSMHarnessImpl.super.onEvent(event, parms);
				} catch (TooBusyException e) {
					throw new RuntimeException(e);
				} 
			}
			
		});
	}
}
