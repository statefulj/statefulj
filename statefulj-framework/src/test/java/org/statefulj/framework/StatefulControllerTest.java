package org.statefulj.framework;

import java.lang.reflect.InvocationTargetException;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.statefulj.framework.controllers.UserController;
import org.statefulj.framework.model.User;
import org.statefulj.framework.utils.ReflectionUtils;

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
		User user = ReflectionUtils.invoke(userControllerMVCProxy, "$_first", User.class);
		
		assertNotNull(user);
		assertTrue(user.getId() > 0);
		assertEquals(UserController.TWO_STATE, user.getState());
		
		// Verify transition from TWO_STATE to THREE_STATE
		//
		user = ReflectionUtils.invoke(userControllerMVCProxy, "$_id_second", User.class, user.getId());
		
		assertTrue(user.getId() > 0);
		assertEquals(UserController.THREE_STATE, user.getState());
	}

}
