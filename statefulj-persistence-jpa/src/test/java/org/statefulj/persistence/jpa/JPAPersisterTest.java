package org.statefulj.persistence.jpa;

import static org.junit.Assert.*;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.statefulj.fsm.Persister;
import org.statefulj.fsm.StaleStateException;
import org.statefulj.fsm.model.State;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/applicationContext-JPAPersisterTests.xml"})
public class JPAPersisterTest {
	
	@Resource
	Persister<Order> jpaPersister;
	
	@Resource
	OrderRepository orderRepo;
	
	@Resource
	State<Order> stateA;
	
	@Resource
	State<Order> stateB;

	@Resource
	State<Order> stateC;
	
	@Test
	public void testValidStateChange() throws StaleStateException {
		Order order = new Order();
		order.setAmount(20);
		orderRepo.save(order);

		State<Order> currentState = jpaPersister.getCurrent(order);
		assertEquals(stateA, currentState);
		
		jpaPersister.setCurrent(order, stateA, stateB);
		assertEquals(order.getState(), stateB.getName());
		
		Order dbOrder = orderRepo.findOne(order.getId());
		assertEquals(order.getState(), dbOrder.getState());

		jpaPersister.setCurrent(order, stateB, stateC);
		assertEquals(order.getState(), stateC.getName());
		
		dbOrder = orderRepo.findOne(order.getId());
		assertEquals(order.getState(), dbOrder.getState());
	}

	@Test(expected=StaleStateException.class)
	public void testInvalidStateChange() throws StaleStateException {
		Order order = new Order();
		order.setAmount(20);
		orderRepo.save(order);

		State<Order> currentState = jpaPersister.getCurrent(order);
		assertEquals(stateA, currentState);
		
		jpaPersister.setCurrent(order, stateB, stateC);
	}
}
