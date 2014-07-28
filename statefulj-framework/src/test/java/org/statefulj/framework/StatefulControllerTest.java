package org.statefulj.framework;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.statefulj.framework.controllers.UserController;
import org.statefulj.framework.model.User;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/applicationContext-StatefulControllerTests.xml"})
public class StatefulControllerTest {
	
	@Autowired
	ApplicationContext appContext;
	
	@Test
	public void testStateTransitions() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		// Make sure proxy is constructed
		//
		Object userControllerMVCProxy = this.appContext.getBean("userControllerMVCProxy");
		assertNotNull(userControllerMVCProxy);
		
		// Verify new User scenario
		//
		Method method = userControllerMVCProxy.getClass().getDeclaredMethod("$_first");
		User user = (User)method.invoke(userControllerMVCProxy, (Object[])null);
		
		assertNotNull(user);
		assertTrue(user.getId() > 0);
		assertEquals(UserController.TWO_STATE, user.getState());
		
		// Verify transition from TWO_STATE to THREE_STATE
		//
		method = userControllerMVCProxy.getClass().getDeclaredMethod("$_id_second", Long.class);
		user = (User)method.invoke(userControllerMVCProxy, new Object[]{ user.getId() });
		
		assertNotNull(method);
		assertTrue(user.getId() > 0);
		assertEquals(UserController.THREE_STATE, user.getState());
	}

}
