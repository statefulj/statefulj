package org.statefulj.framework;

import java.lang.reflect.InvocationTargetException;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.statefulj.framework.controllers.UserController;
import org.statefulj.framework.model.User;
import org.statefulj.framework.utils.ReflectionUtils;
import org.statefulj.framework.utils.UnitTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/applicationContext-StatefulControllerTests.xml"})
public class StatefulControllerTest {
	
	@Resource
	JpaTransactionManager transactionManager;
	
	@Autowired
	ApplicationContext appContext;
	
	@Test
	public void testStateTransitions() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		UnitTestUtils.startTransaction(transactionManager);
		
		// Make sure proxy is constructed
		//
		Object userControllerMVCProxy = this.appContext.getBean("userControllerMVCProxy");
		assertNotNull(userControllerMVCProxy);
		
		// Verify new User scenario
		//
		User user = ReflectionUtils.invoke(userControllerMVCProxy, "$_first", User.class);

		UnitTestUtils.commitTransaction(transactionManager);
		UnitTestUtils.startTransaction(transactionManager);
		
		assertNotNull(user);
		assertTrue(user.getId() > 0);
		assertEquals(UserController.TWO_STATE, user.getState());
		
		// Verify "any" scenario
		//
		user = ReflectionUtils.invoke(userControllerMVCProxy, "$_id_any", User.class, user.getId());
		
		assertNotNull(user);
		assertTrue(user.getId() > 0);
		assertEquals(UserController.TWO_STATE, user.getState());
		
		// Verify transition from TWO_STATE to THREE_STATE
		//
		user = ReflectionUtils.invoke(userControllerMVCProxy, "$_id_second", User.class, user.getId());

		UnitTestUtils.commitTransaction(transactionManager);
		UnitTestUtils.startTransaction(transactionManager);
		
		assertTrue(user.getId() > 0);
		assertEquals(UserController.THREE_STATE, user.getState());

		// Verify "any" scenario
		//
		user = ReflectionUtils.invoke(userControllerMVCProxy, "$_id_any", User.class, user.getId());
		
		assertNotNull(user);
		assertTrue(user.getId() > 0);
		assertEquals(UserController.THREE_STATE, user.getState());
		
	}

}
