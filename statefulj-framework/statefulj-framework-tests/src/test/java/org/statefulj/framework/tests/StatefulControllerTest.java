package org.statefulj.framework.tests;

import java.lang.reflect.InvocationTargetException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.statefulj.framework.core.annotations.FSM;
import org.statefulj.framework.core.model.ReferenceFactory;
import org.statefulj.framework.core.model.FSMHarness;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.framework.core.model.impl.ReferenceFactoryImpl;
import org.statefulj.framework.tests.controllers.UserController;
import org.statefulj.framework.tests.dao.UserRepository;
import org.statefulj.framework.tests.model.User;
import org.statefulj.framework.tests.utils.ReflectionUtils;
import org.statefulj.fsm.TooBusyException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/applicationContext-StatefulControllerTests.xml"})
public class StatefulControllerTest {
	
	@Resource
	ApplicationContext appContext;
	
	@Resource
	UserRepository userRepo;
	
	@Resource(name="userController.fsmHarness")
	FSMHarness fsmHarness;
	
	@FSM
	StatefulFSM<User> fsm;
	
	// TODO : Need to test for annotated parameters
	
	@Test
	public void testStateTransitions() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, TooBusyException {
		
		assertNotNull(fsm);

		ReferenceFactory refFactory = new ReferenceFactoryImpl("userController");
		
		// Make sure proxy is constructed
		//
		Object mvcBinder = this.appContext.getBean(refFactory.getBinderId("springmvc"));
		Object camelBinder = this.appContext.getBean(refFactory.getBinderId("camel"));
		assertNotNull(mvcBinder);
		assertNotNull(camelBinder);
		
		// Verify new User scenario
		//
		HttpServletRequest context = mock(HttpServletRequest.class);
		User user = ReflectionUtils.invoke(mvcBinder, "$_get_first", User.class, context );

		assertNotNull(user);
		assertTrue(user.getId() > 0);
		assertEquals(UserController.TWO_STATE, user.getState());
		
		// Verify "any" scenario
		//
		user = ReflectionUtils.invoke(mvcBinder, "$_get_id_any", User.class, user.getId(), context);
		
		assertNotNull(user);
		assertTrue(user.getId() > 0);
		assertEquals(UserController.TWO_STATE, user.getState());
		
		// Verify transition from TWO_STATE to THREE_STATE
		//
		user = ReflectionUtils.invoke(mvcBinder, "$_post_id_second", User.class, user.getId(), context);

		assertTrue(user.getId() > 0);
		assertEquals(UserController.THREE_STATE, user.getState());

		// Verify "any" scenario
		//
		user = ReflectionUtils.invoke(mvcBinder, "$_get_id_any", User.class, user.getId(), context);
		
		assertNotNull(user);
		assertTrue(user.getId() > 0);
		assertEquals(UserController.THREE_STATE, user.getState());
		
		// Verify "any" scenario
		//
		Object nulObj = ReflectionUtils.invoke(mvcBinder, "$_get_id_four", User.class, user.getId(), context);
		
		assertNull(nulObj);
		user = userRepo.findOne(user.getId());
		assertEquals(UserController.FOUR_STATE, user.getState());

		fsmHarness.onEvent("five", user.getId(), new Object[]{context});
		user = userRepo.findOne(user.getId());
		assertEquals(UserController.FIVE_STATE, user.getState());

		String retVal = ReflectionUtils.invoke(mvcBinder, "$_handleError", String.class, new Exception());
		assertEquals("called", retVal);
		
		ReflectionUtils.invoke(camelBinder, "$_camelone", user.getId());
		ReflectionUtils.invoke(camelBinder, "$_six", user.getId());
		user = userRepo.findOne(user.getId());
		assertEquals(UserController.SIX_STATE, user.getState());

	}

}
