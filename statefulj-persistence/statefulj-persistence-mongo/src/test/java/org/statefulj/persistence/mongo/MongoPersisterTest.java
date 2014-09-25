package org.statefulj.persistence.mongo;

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
@ContextConfiguration({"/applicationContext-MongoPersisterTests.xml"})
public class MongoPersisterTest {
	
	@Resource
	Persister<Order> mongoPersister;
	
	@Resource
	OrderRepository orderRepo;
	
	@Resource
	State<Order> stateA;
	
	@Resource
	State<Order> stateB;

	@Resource
	State<Order> stateC;
	
	@Test
	public void testValidStateChange() throws StaleStateException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		
		// Verify that a new Order without a state set, we return the Start State
		//
		Order order = new Order();
		order.setAmount(20);
		order = this.orderRepo.save(order);
		
		assertNotNull(order.getStateDocument());

		State<Order> currentState = mongoPersister.getCurrent(order);
		assertEquals(stateA, currentState); 
		
		// Verify that qualified a change in state works
		//
		mongoPersister.setCurrent(order, stateA, stateB);
		
		assertEquals(order.getStateDocument().getState(), stateB.getName());

		order = this.orderRepo.save(order);
		Order dbOrder = orderRepo.findOne(order.getId());
		assertEquals(order.getStateDocument().getState(), dbOrder.getStateDocument().getState());

		mongoPersister.setCurrent(order, stateB, stateC);
		assertEquals(order.getStateDocument().getState(), stateC.getName());
		
		dbOrder = orderRepo.findOne(order.getId());
		assertEquals(order.getStateDocument().getState(), dbOrder.getStateDocument().getState());
		
		// Verify that updating the state without going through the Persister doesn't work
		//
		order = new Order();
		mongoPersister.setCurrent(order, stateA, stateB);
		order = this.orderRepo.save(order);

		dbOrder = orderRepo.findOne(order.getId());
		currentState = mongoPersister.getCurrent(dbOrder);
		assertEquals(stateB, currentState);
		
	}

	@Test(expected=StaleStateException.class)
	public void testInvalidStateChange() throws StaleStateException {
		Order order = new Order();
		order.setAmount(20);
		
		order = orderRepo.save(order);
		
		State<Order> currentState = mongoPersister.getCurrent(order);
		
		assertEquals(stateA, currentState);
		
		try {
			mongoPersister.setCurrent(order, stateB, stateC);
		} catch(StaleStateException e) {
			assertEquals(stateA.getName(), order.getStateDocument().getState());
			throw e;
		}
	}
}
