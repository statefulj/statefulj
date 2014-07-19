package org.statefulj.fsm;

import static org.junit.Assert.*;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Test;
import org.statefulj.fsm.FSM;
import org.statefulj.fsm.RetryException;
import org.statefulj.fsm.TooBusyException;
import org.statefulj.fsm.model.Action;
import org.statefulj.fsm.model.State;
import org.statefulj.fsm.model.StateActionPair;
import org.statefulj.fsm.model.Transition;
import org.statefulj.fsm.model.impl.DeterministicTransitionImpl;
import org.statefulj.fsm.model.impl.StateActionPairImpl;
import org.statefulj.fsm.model.impl.StateImpl;
import org.statefulj.fsm.model.impl.WaitAndRetryActionImpl;
import org.statefulj.persistence.memory.MemoryPersisterImpl;

import static org.mockito.Mockito.*;

public class FSMTest {


	@SuppressWarnings("unchecked")
	@Test
	public void testSimpleFSM() throws TooBusyException, RetryException {
		// Stateful Object
		//
		final Object stateful = new Object();
		
		// Set up the Actions as Mock so we can inspect them
		//
		Action<Object> actionA = mock(Action.class);
		Action<Object> actionB = mock(Action.class);
		
		// Events
		//
		String eventA = "eventA";
		String eventB = "eventB";
		
		// States
		//
		StateImpl<Object> stateA = new StateImpl<Object>("stateA");
		StateImpl<Object> stateB = new StateImpl<Object>("stateB");
		StateImpl<Object> stateC = new StateImpl<Object>("stateC", true); // End State
		
		// Transitions
		//
		stateA.addTransition(eventA, new DeterministicTransitionImpl<Object>(stateB, actionA));
		stateB.addTransition(eventB, new DeterministicTransitionImpl<Object>(stateC, actionB));

		// FSM
		//
		MemoryPersisterImpl<Object> persister = new MemoryPersisterImpl<Object>(stateful, stateA);
		FSM<Object> fsm = new FSM<Object>("SimpleFSM", persister);

		// Verify that on eventA, we transition to StateB and verify
		// that we call actionA with the correct arg
		//
		Object arg = new Object();
		State<Object> current = fsm.onEvent(stateful, eventA, arg);
		assertEquals(stateB, current);
		assertFalse(current.isEndState());
		verify(actionA).execute(stateful, eventA, arg);
		verify(actionB, never()).execute(stateful, eventA, arg);
		
		reset(actionA);
		
		// Verify that on eventA from StateB, that nothing happened
		//
		current = fsm.onEvent(stateful, eventA, arg);
		assertEquals(stateB, current);
		assertFalse(current.isEndState());
		verify(actionA, never()).execute(stateful, eventA, arg);
		verify(actionB, never()).execute(stateful, eventA, arg);
		
		// Verify that on eventB from StateB, we transition to stateC - the endState, and
		// call actionB with the correct args
		//
		current = fsm.onEvent(stateful, eventB, arg);
		assertEquals(stateC, current);
		assertTrue(current.isEndState());
		verify(actionA, never()).execute(stateful, eventB, arg);
		verify(actionB).execute(stateful, eventB, arg);
		
	}
	
	@Test
	public void testNonDeterminsticTransition() throws TooBusyException {
		// Statefull Object
		//
		final Object stateful = new Object();

		final MutableInt eventCnt = new MutableInt(0);
		
		// Events
		//
		String eventA = "eventA";
		
		// States
		//
		final StateImpl<Object> stateA = new StateImpl<Object>("stateA");
		final StateImpl<Object> stateB = new StateImpl<Object>("stateB", true);
		
		// Transitions
		//
		stateA.addTransition(eventA, new Transition<Object>() {
			
			public StateActionPair<Object> getStateActionPair() {
				State<Object> next = null;
				if (eventCnt.intValue() < 2) {
					next = stateA;
				} else {
					next = stateB;
				}
				eventCnt.add(1);
				return new StateActionPairImpl<Object>(next, null);
			}
		});
		
		// FSM
		//
		MemoryPersisterImpl<Object> persister = new MemoryPersisterImpl<Object>(stateful, stateA);
		final FSM<Object> fsm = new FSM<Object>("NDFSM", persister);

		// Verify that the first two eventA returns stateA
		//
		State<Object> current = fsm.onEvent(stateful, eventA);
		assertEquals(stateA, current);

		current = fsm.onEvent(stateful, eventA);
		assertEquals(stateA, current);

		// Third should return stateB
		//
		current = fsm.onEvent(stateful, eventA);
		assertEquals(stateB, current);

	}
	
	@Test
	public void testConcurrency() throws TooBusyException, InterruptedException, RetryException {
		// Stateful Object
		//
		final Object stateful = new Object();
		
		// FSM
		//
		MemoryPersisterImpl<Object> persister = new MemoryPersisterImpl<Object>();
		final FSM<Object> fsm = new FSM<Object>("ConcurrentFSM", persister);

		// Events
		//
		final String eventA = "eventA";
		final String eventB = "eventB";
		
		WaitAndRetryActionImpl<Object> wra = new WaitAndRetryActionImpl<Object>(100); // wait stateful00 ms and retry
		
		// This action will wait for 300ms then invoke the fsm with eventA.
		// This should transition the FSM back to stateA
		//
		Action<Object> waitAction = new Action<Object>() {
			
			public void execute(Object obj, String event, Object... args) throws RetryException {
				try {
					Thread.sleep(250);
					fsm.onEvent(stateful, eventA, args);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				} catch (TooBusyException tbe) {
					throw new RuntimeException(tbe);
				}
				
			}
		};
		
		// Add Spies
		//
		WaitAndRetryActionImpl<Object> wraSpy = spy(wra);
		Action<Object> waitActionSpy = spy(waitAction);
		
		// States
		//
		StateImpl<Object> stateA = new StateImpl<Object>("stateA");
		StateImpl<Object> statePending = new StateImpl<Object>("stateEnd");
		StateImpl<Object> stateEnd = new StateImpl<Object>("stateEnd");
		
		// Transitions
		//
		stateA.addTransition(eventA, new DeterministicTransitionImpl<Object>(statePending, waitActionSpy));
		stateA.addTransition(eventB, new DeterministicTransitionImpl<Object>(stateEnd));
		
		statePending.addTransition(eventA, new DeterministicTransitionImpl<Object>(stateA));
		statePending.addTransition(eventB, new DeterministicTransitionImpl<Object>(statePending, wraSpy));
		
		// Set start to stateA
		//
		persister.setCurrent(stateful, stateA);
		
		
		// Kick off a thread that will invoke the fsm with eventA
		//
		final Object args = null;
		new Thread(new Runnable() {
			
			public void run() {
				try {
					fsm.onEvent(stateful, eventA, args);
				} catch (TooBusyException e) {
					throw new RuntimeException(e);
				}
			}
		}).start();
		
		// Give eventA some time
		//
		Thread.sleep(5);
		
		// Fire off eventB
		//
		State<Object> current = fsm.onEvent(stateful, eventB, args);

		// Verify that we are at the end state
		//
		assertEquals(stateEnd, current);

		// Make sure we retried eventB at least 2 times (possibly more)
		//
		verify(wraSpy, atLeast(2)).execute(stateful, eventB, args);

		// Make sure we only called waitAction once
		//
		verify(waitActionSpy, times(1)).execute(stateful, eventA, args);
		
	}
	
	@Test(expected=TooBusyException.class)
	public void testTooBusy() throws TooBusyException {
		// Stateful Object
		//
		final Object stateful = new Object();

		// Events
		//
		final String eventA = "eventA";

		// Actions
		//
		Action<Object> throwAction = new Action<Object>() {
			
			public void execute(Object obj, String event, Object... args) throws RetryException {
				throw new RetryException();
			}
		};
		
		// States
		//
		State<Object> stateA = new StateImpl<Object>("stateA");
		
		// Transitions
		//
		stateA.addTransition(eventA, new DeterministicTransitionImpl<Object>(stateA, throwAction));

		// FSM
		//
		MemoryPersisterImpl<Object> persister = new MemoryPersisterImpl<Object>(stateful, stateA);
		final FSM<Object> fsm = new FSM<Object>("TooBusy", persister);
		fsm.setRetries(1);

		// Boom
		//
		fsm.onEvent(stateful, eventA);
	}

}
