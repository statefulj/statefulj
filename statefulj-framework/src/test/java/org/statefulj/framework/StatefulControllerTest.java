package org.statefulj.framework;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/applicationContext-StatefulControllerTests.xml"})
public class StatefulControllerTest {
	
	@Autowired
	ApplicationContext appContext;
	
	@Test
	public void testController() {
		Object userControllerMVCProxy = this.appContext.getBean("userControllerMVCProxy");
		assertNotNull(userControllerMVCProxy);
	}

}
