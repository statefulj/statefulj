package org.statefulj.framework.tests;

import java.lang.reflect.InvocationTargetException;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.statefulj.framework.core.model.ReferenceFactory;
import org.statefulj.framework.core.model.impl.ReferenceFactoryImpl;
import org.statefulj.framework.tests.controllers.UserController;
import org.statefulj.framework.tests.dao.UserRepository;
import org.statefulj.framework.tests.model.User;
import org.statefulj.framework.tests.utils.ReflectionUtils;
import org.statefulj.framework.tests.utils.UnitTestUtils;
import org.statefulj.fsm.FSM;
import org.statefulj.fsm.TooBusyException;
import org.statefulj.fsm.model.State;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/applicationContext-StatefulControllerTests.xml"})
public class StatefulControllerTest {
	
	@Resource
	JpaTransactionManager transactionManager;
	
	@Resource
	ApplicationContext appContext;
	
	@Resource
	UserRepository userRepo;
	
	@Resource
	FSM<User> userFSM;
	
	// TODO : Need to test for annotated parameters
	
	@Test
	public void testStateTransitions() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, TooBusyException {
		UnitTestUtils.startTransaction(transactionManager);
		
		ReferenceFactory refFactory = new ReferenceFactoryImpl("userController");
		
		// Make sure proxy is constructed
		//
		Object userControllerBinder = this.appContext.getBean(refFactory.getBinderId());
		assertNotNull(userControllerBinder);
		
		// Verify new User scenario
		//
		User user = ReflectionUtils.invoke(userControllerBinder, "$_get_first", User.class);

		UnitTestUtils.commitTransaction(transactionManager);
		UnitTestUtils.startTransaction(transactionManager);
		
		assertNotNull(user);
		assertTrue(user.getId() > 0);
		assertEquals(UserController.TWO_STATE, user.getState());
		
		// Verify "any" scenario
		//
		user = ReflectionUtils.invoke(userControllerBinder, "$_get_id_any", User.class, user.getId());
		
		assertNotNull(user);
		assertTrue(user.getId() > 0);
		assertEquals(UserController.TWO_STATE, user.getState());
		
		// Verify transition from TWO_STATE to THREE_STATE
		//
		user = ReflectionUtils.invoke(userControllerBinder, "$_post_id_second", User.class, user.getId());

		UnitTestUtils.commitTransaction(transactionManager);
		UnitTestUtils.startTransaction(transactionManager);
		
		assertTrue(user.getId() > 0);
		assertEquals(UserController.THREE_STATE, user.getState());

		// Verify "any" scenario
		//
		user = ReflectionUtils.invoke(userControllerBinder, "$_get_id_any", User.class, user.getId());
		
		assertNotNull(user);
		assertTrue(user.getId() > 0);
		assertEquals(UserController.THREE_STATE, user.getState());
		
		UnitTestUtils.commitTransaction(transactionManager);
		UnitTestUtils.startTransaction(transactionManager);

		// Verify "any" scenario
		//
		Object nulObj = ReflectionUtils.invoke(userControllerBinder, "$_get_id_four", User.class, user.getId());
		
		assertNull(nulObj);
		user = userRepo.findOne(user.getId());
		assertEquals(UserController.FOUR_STATE, user.getState());

		State<User> nextState = userFSM.onEvent(user, "five");
		assertNotNull(nextState);
		assertEquals(UserController.FIVE_STATE, nextState.getName());

		user = userRepo.findOne(user.getId());
		assertEquals(UserController.FIVE_STATE, user.getState());

		UnitTestUtils.commitTransaction(transactionManager);

	}

}
