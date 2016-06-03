package org.statefulj.fsm;

import org.junit.Before;
import org.junit.Test;
import org.statefulj.fsm.model.Action;
import org.statefulj.fsm.model.StateActionPair;
import org.statefulj.fsm.model.Transition;
import org.statefulj.fsm.model.impl.StateActionPairImpl;
import org.statefulj.fsm.model.impl.StateImpl;
import org.statefulj.persistence.annotations.State;

import static org.junit.Assert.*;

/**
 * Created by andrewhall on 6/2/16.
 */
public class FSMBuilderTest {

    class FooState {

        @State
        public String state;

        public String msg;
    }

    FSM<FooState> fooStateFSM;

    @Before
    public void setUp() {
        Action action = new Action() {
                    @Override
                    public void execute(Object stateful, String event, Object... args) throws RetryException {
                        ((FooState)stateful).msg = "Action Happened";
                    }
                };

        FSM.FSMBuilder<FooState> fsmBuilder = FSM.FSMBuilder.newBuilder(FooState.class);

        fsmBuilder.
                buildState("FOO")
                    .addTransition("noop", "FOO")
                    .addTransition("to-bar", "BAR")
                    .addTransition("to-baz", "BAZ")
                .done()
                .buildState("BAR")
                    .addTransition("noop", "BAR")
                    .addTransition("to-foo", "FOO")
                    .addTransition("to-baz", "BAZ")
                .done()
                .buildState("BAZ")
                    .setEndState(true)
                    .addTransition("noop", "BAZ")
                    .addTransition("to-bar", "BAR")
                    .addTransition("to-foo", "FOO", action)
                .done();

        this.fooStateFSM = fsmBuilder.build();
    }

    @Test
    public void testStartState() throws TooBusyException {
        FooState fooState = new FooState();
        assertEquals("FOO", this.fooStateFSM.getCurrentState(fooState).getName());
    }

    @Test
    public void testNoop() throws TooBusyException {
        FooState fooState = new FooState();
        this.fooStateFSM.onEvent(fooState, "noop");
        assertEquals("FOO", this.fooStateFSM.getCurrentState(fooState).getName());
    }

    @Test
    public void testToBar() throws TooBusyException {
        FooState fooState = new FooState();
        this.fooStateFSM.onEvent(fooState, "to-bar");
        assertEquals("BAR", this.fooStateFSM.getCurrentState(fooState).getName());
    }

    @Test
    public void testToBaz() throws TooBusyException {
        FooState fooState = new FooState();
        this.fooStateFSM.onEvent(fooState, "to-baz");
        assertEquals("BAZ", this.fooStateFSM.getCurrentState(fooState).getName());
        assertEquals(true, this.fooStateFSM.getCurrentState(fooState).isEndState());
    }

    @Test
    public void testToBarToBaz() throws TooBusyException {
        FooState fooState = new FooState();
        this.fooStateFSM.onEvent(fooState, "to-bar");
        this.fooStateFSM.onEvent(fooState, "to-baz");
        assertEquals("BAZ", this.fooStateFSM.getCurrentState(fooState).getName());
    }

    @Test
    public void testToBarToBaztoBar() throws TooBusyException {
        FooState fooState = new FooState();
        this.fooStateFSM.onEvent(fooState, "to-bar");
        this.fooStateFSM.onEvent(fooState, "to-baz");
        this.fooStateFSM.onEvent(fooState, "to-bar");
        assertEquals("BAR", this.fooStateFSM.getCurrentState(fooState).getName());
    }

    @Test
    public void testAddingState() throws TooBusyException {
        this.fooStateFSM =  FSM.FSMBuilder
                .newBuilder(FooState.class)
                .addState(new StateImpl<FooState>("FOO"))
                .build();

        FooState fooState = new FooState();
        assertEquals("FOO", this.fooStateFSM.getCurrentState(fooState).getName());
    }

    @Test
    public void testBlockingState() throws TooBusyException {
        this.fooStateFSM =  FSM.FSMBuilder
                .newBuilder(FooState.class)
                .buildState("FOO")
                    .setBlockingState(true)
                .done()
                .build();

        FooState fooState = new FooState();
        assertEquals("FOO", this.fooStateFSM.getCurrentState(fooState).getName());
        assertEquals(true, this.fooStateFSM.getCurrentState(fooState).isBlocking());
    }

    @Test
    public void testAddingTransition() throws TooBusyException {
        this.fooStateFSM = FSM.FSMBuilder
                .newBuilder(FooState.class)
                .buildState("FOO")
                    .addTransition(
                            "test",
                            new Transition<FooState>() {

                                @Override
                                public StateActionPair<FooState> getStateActionPair(FooState stateful, String event, Object... args) throws RetryException {
                                    return new StateActionPairImpl<FooState>(
                                            fooStateFSM.getCurrentState(stateful),
                                            new Action<FooState>() {
                                                @Override
                                                public void execute(FooState stateful, String event, Object... args) throws RetryException {
                                                    stateful.msg = "Action called";
                                                }
                                            });
                                }
                            })
                .done()
                .build();


        FooState fooState = new FooState();
        this.fooStateFSM.onEvent(fooState, "test");
        assertEquals("Action called", fooState.msg);
    }
}
