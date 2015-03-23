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
package org.statefulj.persistence.mongo;

import static org.junit.Assert.*;

import javax.annotation.Resource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.statefulj.fsm.Persister;
import org.statefulj.fsm.StaleStateException;
import org.statefulj.fsm.model.State;
import org.statefulj.persistence.mongo.model.StateDocument;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/applicationContext-MongoPersisterTests.xml"})
public class MongoPersisterTest {
	
	@Resource
	private Persister<Order> mongoPersister;
	
	@Resource
	private OrderRepository orderRepo;
	
	@Resource
	private State<Order> stateA;
	
	@Resource
	private State<Order> stateB;

	@Resource
	private State<Order> stateC;
	
	@Resource
	private MongoTemplate mongoTemplate;
	
	@Before
	@After
	public void dropDB() {
		MongoUtils.dropDB(mongoTemplate);
	}
	
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

	@Test
	public void testDeleteCascadeSupport() throws StaleStateException {
		Order order = new Order();
		order.setAmount(20);
		
		order = orderRepo.save(order);
		
		String orderId = order.getId();
		String stateId = order.getStateDocument().getId();

		order = orderRepo.findOne(orderId);
		assertNotNull(order);
		StateDocument state = this.mongoTemplate.findById(stateId, StateDocumentImpl.class);
		assertNotNull(state);
		
		orderRepo.delete(order);
		
		order = orderRepo.findOne(orderId);
		assertNull(order);
		state = this.mongoTemplate.findById(stateId, StateDocumentImpl.class);
		assertNull(state);
	}
}
