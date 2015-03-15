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
package org.statefulj.framework.tests;

import java.lang.reflect.InvocationTargetException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.statefulj.common.utils.ReflectionUtils;
import org.statefulj.framework.core.annotations.FSM;
import org.statefulj.framework.core.model.ReferenceFactory;
import org.statefulj.framework.core.model.FSMHarness;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.framework.core.model.impl.ReferenceFactoryImpl;
import org.statefulj.framework.tests.dao.UserRepository;
import org.statefulj.framework.tests.model.User;

import static org.statefulj.framework.tests.utils.ReflectionUtils.*;

import org.statefulj.fsm.Persister;
import org.statefulj.fsm.StaleStateException;
import org.statefulj.fsm.TooBusyException;
import org.statefulj.fsm.model.State;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/applicationContext-StatefulControllerTests.xml"})
public class StatefulControllerTest {
	
	@Resource
	ApplicationContext appContext;
	
	@Resource
	UserRepository userRepo;
	
	@Resource
	JpaTransactionManager transactionManager;
	
	@Resource(name="userController.fsmHarness")
	FSMHarness userFSMHarness;
	
	@Resource(name="concurrencyController.fsmHarness")
	FSMHarness concurrencyFSMHarness;
	
	@FSM("userController")
	StatefulFSM<User> userFSM;
	
	@FSM("overloadedMethodController")
	StatefulFSM<User> overloadFSM;
	
	// TODO : Need to test for annotated parameters
	
	@Test
	public void testStateTransitions() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, TooBusyException {
		
		assertNotNull(userFSM);

		ReferenceFactory refFactory = new ReferenceFactoryImpl("userController");
		
		// Make sure proxy is constructed
		//
		Object mvcBinder = this.appContext.getBean(refFactory.getBinderId("springmvc"));
		Object jerseyBinder = this.appContext.getBean(refFactory.getBinderId("jersey"));
		Object camelBinder = this.appContext.getBean(refFactory.getBinderId("camel"));
		assertNotNull(mvcBinder);
		assertNotNull(camelBinder);
		
		// Verify new User scenario
		//
		HttpServletRequest context = mock(HttpServletRequest.class);
		User user = invoke(mvcBinder, "$_get_first", User.class, context );

		assertNotNull(user);
		assertTrue(user.getId() > 0);
		assertEquals(User.TWO_STATE, user.getState());
		
		// Verify "any" scenario
		//
		user = invoke(mvcBinder, "$_get_id_any", User.class, user.getId(), context);
		
		assertNotNull(user);
		assertTrue(user.getId() > 0);
		assertEquals(User.TWO_STATE, user.getState());
		
		// Verify transition from TWO_STATE to THREE_STATE
		//
		user = invoke(mvcBinder, "$_post_id_second", User.class, user.getId(), context);

		assertTrue(user.getId() > 0);
		assertEquals(User.THREE_STATE, user.getState());

		// Verify "any" scenario
		//
		user = invoke(mvcBinder, "$_get_id_any", User.class, user.getId(), context);
		
		assertNotNull(user);
		assertTrue(user.getId() > 0);
		assertEquals(User.THREE_STATE, user.getState());
		
		// Verify "any" scenario
		//
		Object nulObj = invoke(mvcBinder, "$_get_id_four", User.class, user.getId(), context);
		
		assertNull(nulObj);
		user = userRepo.findOne(user.getId());
		assertEquals(User.FOUR_STATE, user.getState());

		userFSMHarness.onEvent("five", user.getId(), new Object[]{context});
		user = userRepo.findOne(user.getId());
		assertEquals(User.FIVE_STATE, user.getState());

		assertEquals(
				mvcBinder.getClass().getMethod("$_handleError", Exception.class), 
				ReflectionUtils.getFirstAnnotatedMethod(
						mvcBinder.getClass(), 
						ExceptionHandler.class));
		
		String retVal = invoke(mvcBinder, "$_handleError", String.class, new Exception());
		assertEquals("called", retVal);
		
		invoke(camelBinder, "$_camelone", user.getId());
		invoke(camelBinder, "$_six", user.getId());
		user = userRepo.findOne(user.getId());
		assertEquals(User.SIX_STATE, user.getState());

		User retUser = invoke(jerseyBinder, "$_get_id_one", User.class, user.getId(), context);
		assertNotNull(retUser);
	}
	
	@Test
	public void testOverloadedMethod() throws TooBusyException {
		assertNotNull(overloadFSM);
		
		User user = new User();
		String response = (String)overloadFSM.onEvent(user, "one");
		assertEquals("method1", response);

		response = (String)overloadFSM.onEvent(user, "two", "foo");
		assertEquals("method2", response);

		response = (String)overloadFSM.onEvent(user, "three", 1);
		assertEquals("method3", response);
	}

	@SuppressWarnings("unchecked")
	@Test(expected=TooBusyException.class)
	public void testBlockedState() throws TooBusyException, StaleStateException {
		
		assertNotNull(userFSM);

		ReferenceFactory refFactory = new ReferenceFactoryImpl("userController");

		// Create a User and force it to SIX_STATE
		//
		User user = new User();
		user = userRepo.save(user);
		
		State<User> stateSix = (State<User>)this.appContext.getBean(refFactory.getStateId(User.SIX_STATE));
		Persister<User> persister = (Persister<User>)this.appContext.getBean(refFactory.getPersisterId());
		persister.setCurrent(user, persister.getCurrent(user), stateSix);
		
		assertEquals(stateSix, persister.getCurrent(user));

		// Now kick off an event, it should block and then eventually throw a TooBusyException
		//
		org.statefulj.framework.core.fsm.FSM<User> fsm = (org.statefulj.framework.core.fsm.FSM<User>)this.appContext.getBean(refFactory.getFSMId());
		fsm.setRetryAttempts(1);
		fsm.onEvent(user, "block.me");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testTransitionOutOfBlocking() throws TooBusyException, StaleStateException {

		assertNotNull(userFSM);

		// Create a User and force it to SIX_STATE
		//
		final User user = userRepo.save(new User());
		
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		tt.execute(new TransactionCallback<Object>() {

			@Override
			public Object doInTransaction(TransactionStatus status) {
				try {
					ReferenceFactory refFactory = new ReferenceFactoryImpl("userController");
					User dbUser = userRepo.findOne(user.getId());
					State<User> stateSix = (State<User>)appContext.getBean(refFactory.getStateId(User.SIX_STATE));
					Persister<User> persister = (Persister<User>)appContext.getBean(refFactory.getPersisterId());
					persister.setCurrent(dbUser, persister.getCurrent(user), stateSix);

					// Spawn another Thread
					//
					new Thread(new Runnable() {
						
						@Override
						public void run() {
							try {
								Thread.sleep(1500);
								TransactionTemplate tt = new TransactionTemplate(transactionManager);
								tt.execute(new TransactionCallback<Object>() {

									@Override
									public Object doInTransaction(TransactionStatus status) {
										try {
											User dbUser = userRepo.findOne(user.getId());
											userFSM.onEvent(dbUser, "unblock");
											return null;
										} catch (TooBusyException e) {
											throw new RuntimeException(e);
										}
									}
									
								});
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}).start();

					userFSM.onEvent(dbUser, "this-should-block");
					return null;
				} catch (TooBusyException e) {
					throw new RuntimeException(e);
				} catch (StaleStateException e) {
					throw new RuntimeException(e);
				}
			}
			
		});
		
		User dbUser = userRepo.findOne(user.getId());
		
		assertEquals(User.SEVEN_STATE, dbUser.getState());
	}

	@Test
	public void testConcurrency() throws TooBusyException, StaleStateException, InterruptedException, InstantiationException {

		final User user = userRepo.save(new User());
		
		// Spawn another Thread
		//
		final Object monitor = new Object();
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				synchronized(monitor) {
					try {
						concurrencyFSMHarness.onEvent("two", new Object[]{user.getId(), null, monitor});
					} catch(Exception e) {
						throw new RuntimeException(e);
					} finally {
						monitor.notify();
					}
				}
			}
		});
		synchronized(monitor) {
			t.start();
			concurrencyFSMHarness.onEvent("one", new Object[]{user.getId(), null, monitor});
		}
		User user2 = userRepo.findOne(user.getId());
		assertEquals(User.THREE_STATE, user2.getState());
	}
}
