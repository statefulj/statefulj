package org.statefulj.fsm.persistence;

import org.junit.Test;
import org.mockito.Mockito;
import org.statefulj.common.utils.ReflectionUtils;
import org.statefulj.persistence.StateFieldAccessor;
import org.statefulj.persistence.annotations.State;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

/**
 * Created by andrewhall on 7/24/16.
 */
public class StateFieldAccessorTests {

    static class StatefulClass1 {

        @State
        private String state;

    }

    static class StatefulClass2 {

        @State(accessorType = State.AccessorType.METHOD)
        private String state;

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }
    }

    static class StatefulClass3 {

        @State(accessorType = State.AccessorType.METHOD, getMethodName = "nonBeanGetState", setMethodName = "nonBeanSetState")
        private String state;

        public String nonBeanGetState() {
            return state;
        }

        public void nonBeanSetState(String state) {
            this.state = state;
        }
    }

    static class StatefulClass4 {

        @State(accessorType = State.AccessorType.METHOD)
        private String state;
    }

    static class StatefulClass5 {

        @State(accessorType = State.AccessorType.METHOD)
        private String state;

        public String nonBeanGetState() {
            return state;
        }

    }

    static class StatefulClass6 {

        @State(accessorType = State.AccessorType.PROPERTY)
        private String state;
    }

    @Test
    public void testStatePersisterStatefulClass1() {
        StateFieldAccessor<StatefulClass1> statePersister = new StateFieldAccessor(
                StatefulClass1.class,
                ReflectionUtils.getFirstAnnotatedField(StatefulClass1.class, State.class));
        assertNotNull(statePersister.getField());
        assertEquals(null, statePersister.getGetMethod());
        assertEquals(null, statePersister.getSetMethod());

        Field stateField = statePersister.getField();
        assertEquals("state", stateField.getName());
    }

    @Test
    public void testStatePersisterStatefulClass2() {
        StateFieldAccessor<StatefulClass2> statePersister = new StateFieldAccessor(
                StatefulClass2.class,
                ReflectionUtils.getFirstAnnotatedField(StatefulClass2.class, State.class));
        assertNotNull(statePersister.getField());
        assertEquals("getState", statePersister.getGetMethod().getName());
        assertEquals("setState", statePersister.getSetMethod().getName());

        Field stateField = statePersister.getField();
        assertEquals("state", stateField.getName());
    }

    @Test
    public void testStatePersisterStatefulClass3() {
        StateFieldAccessor<StatefulClass3> statePersister = new StateFieldAccessor(
                StatefulClass3.class,
                ReflectionUtils.getFirstAnnotatedField(StatefulClass3.class, State.class));
        assertNotNull(statePersister.getField());
        assertEquals("nonBeanGetState", statePersister.getGetMethod().getName());
        assertEquals("nonBeanSetState", statePersister.getSetMethod().getName());

        Field stateField = statePersister.getField();
        assertEquals("state", stateField.getName());
    }

    @Test(expected = RuntimeException.class)
    public void testStatePersisterStatefulClass4() {
        new StateFieldAccessor(
                StatefulClass4.class,
                ReflectionUtils.getFirstAnnotatedField(StatefulClass4.class, State.class));
    }

    @Test(expected = RuntimeException.class)
    public void testStatePersisterStatefulClass5() {
        new StateFieldAccessor(
                StatefulClass5.class,
                ReflectionUtils.getFirstAnnotatedField(StatefulClass5.class, State.class));
    }

    @Test
    public void testGetterSetterStatefulClass1() {
        StateFieldAccessor<StatefulClass1> statePersister = new StateFieldAccessor(
                StatefulClass1.class,
                ReflectionUtils.getFirstAnnotatedField(StatefulClass1.class, State.class));
        StatefulClass1 stateful = new StatefulClass1();
        assertNull(statePersister.getValue(stateful));
        statePersister.setValue(stateful, "testValue");
        assertEquals("testValue", statePersister.getValue(stateful));
    }

    @Test
    public void testGetterSetterStatefulClass2() {
        StateFieldAccessor<StatefulClass2> statePersister = new StateFieldAccessor(
                StatefulClass2.class,
                ReflectionUtils.getFirstAnnotatedField(StatefulClass2.class, State.class));
        StatefulClass2 stateful = new StatefulClass2();

        StatefulClass2 spy = Mockito.spy(stateful);
        assertNull(statePersister.getValue(spy));
        Mockito.verify(spy).getState();

        statePersister.setValue(spy, "testValue");
        Mockito.verify(spy).setState("testValue");
        assertEquals("testValue", statePersister.getValue(spy));
    }

    @Test
    public void testGetterSetterStatefulClass3() {
        StateFieldAccessor<StatefulClass3> statePersister = new StateFieldAccessor(
                StatefulClass3.class,
                ReflectionUtils.getFirstAnnotatedField(StatefulClass3.class, State.class));
        StatefulClass3 stateful = new StatefulClass3();

        StatefulClass3 spy = Mockito.spy(stateful);
        assertNull(statePersister.getValue(spy));
        Mockito.verify(spy).nonBeanGetState();

        statePersister.setValue(spy, "testValue");
        Mockito.verify(spy).nonBeanSetState("testValue");
        assertEquals("testValue", statePersister.getValue(spy));
    }

    @Test
    public void testStatePersisterStatefulClass6() {
        StateFieldAccessor<StatefulClass6> statePersister = new StateFieldAccessor(
                StatefulClass6.class,
                ReflectionUtils.getFirstAnnotatedField(StatefulClass6.class, State.class));
        assertNotNull(statePersister.getField());
        assertEquals(null, statePersister.getGetMethod());
        assertEquals(null, statePersister.getSetMethod());

        Field stateField = statePersister.getField();
        assertEquals("state", stateField.getName());
    }


}
