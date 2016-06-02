package org.statefulj.fsm;

import org.junit.Before;
import org.junit.Test;
import org.statefulj.fsm.model.Action;
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
        FSM.FSMBuilder fsmBuilder = FSM.FSMBuilder.newBuilder();

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
                    .addTransition("noop", "BAZ")
                    .addTransition("to-bar", "BAR")
                    .addTransition("to-foo", "FOO",
                            new Action() {
                                @Override
                                public void execute(Object stateful, String event, Object... args) throws RetryException {
                                    ((FooState)stateful).msg = "Action Happened";
                                }
                            })
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
    public void testToBarToBazToFoo() throws TooBusyException {
        FooState fooState = new FooState();
        this.fooStateFSM.onEvent(fooState, "to-bar");
        this.fooStateFSM.onEvent(fooState, "to-baz");
        this.fooStateFSM.onEvent(fooState, "to-foo");
        assertEquals("FOO", this.fooStateFSM.getCurrentState(fooState).getName());
        assertEquals("Action Happened", fooState.msg);
    }
}
