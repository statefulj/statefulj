package org.statefulj.common.utils;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * Created by andrewhall on 8/19/16.
 */
public class FieldAccessorTests {

    static class TestClass1 {

        private String field;

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }
    }

    static class TestClass2{

        private String field;

        public String getField() {
            return field;
        }

    }

    static class TestClass3 {

        private String field;

        public void setField(String field) {
            this.field = field;
        }
    }

    static class TestClass4 {

        private String field;

    }

    @Test
    public void testTestClass1() throws NoSuchMethodException {
        Field field = ReflectionUtils.getField(TestClass1.class, "field");
        FieldAccessor<TestClass1, String> testClass1StringFieldAccessor =
                new FieldAccessor<TestClass1, String>(TestClass1.class, field);
        Method method = TestClass1.class.getMethod("getField");
        assertEquals(method, testClass1StringFieldAccessor.getGetMethod());
        method = TestClass1.class.getMethod("setField", String.class);
        assertEquals(method, testClass1StringFieldAccessor.getSetMethod());
    }

    @Test
    public void testTestClass2() throws NoSuchMethodException {
        Field field = ReflectionUtils.getField(TestClass2.class, "field");
        FieldAccessor<TestClass2, String> testClass2StringFieldAccessor =
                new FieldAccessor<TestClass2, String>(TestClass2.class, field);
        Method method = TestClass2.class.getMethod("getField");
        assertEquals(method, testClass2StringFieldAccessor.getGetMethod());
        assertNull(testClass2StringFieldAccessor.getSetMethod());
    }

    @Test
    public void testTestClass3() throws NoSuchMethodException {
        Field field = ReflectionUtils.getField(TestClass3.class, "field");
        FieldAccessor<TestClass3, String> testClass3StringFieldAccessor =
                new FieldAccessor<TestClass3, String>(TestClass3.class, field);
        Method method = TestClass3.class.getMethod("setField", String.class);
        assertEquals(method, testClass3StringFieldAccessor.getSetMethod());
        assertNull(testClass3StringFieldAccessor.getGetMethod());
    }

    @Test
    public void testTestClass4() throws NoSuchMethodException {
        Field field = ReflectionUtils.getField(TestClass4.class, "field");
        FieldAccessor<TestClass4, String> testClass4StringFieldAccessor =
                new FieldAccessor<TestClass4, String>(TestClass4.class, field);
        assertNull(testClass4StringFieldAccessor.getSetMethod());
        assertNull(testClass4StringFieldAccessor.getGetMethod());
    }

    @Test
    public void testSetFieldClass1() throws NoSuchMethodException {
        Field field = ReflectionUtils.getField(TestClass1.class, "field");
        FieldAccessor<TestClass1, String> testClass1StringFieldAccessor =
                new FieldAccessor<TestClass1, String>(TestClass1.class, field);
        TestClass1 testClass1 = new TestClass1();
        testClass1StringFieldAccessor.setValue(testClass1, "foo");
        assertEquals("foo", testClass1StringFieldAccessor.getValue(testClass1));
    }

    @Test
    public void testSetFieldClass2() throws NoSuchMethodException {
        Field field = ReflectionUtils.getField(TestClass2.class, "field");
        FieldAccessor<TestClass2, String> testClass2StringFieldAccessor =
                new FieldAccessor<TestClass2, String>(TestClass2.class, field);
        TestClass2 testClass2 = new TestClass2();
        testClass2StringFieldAccessor.setValue(testClass2, "foo");
        assertEquals("foo", testClass2StringFieldAccessor.getValue(testClass2));
    }

    @Test
    public void testSetFieldClass3() throws NoSuchMethodException {
        Field field = ReflectionUtils.getField(TestClass3.class, "field");
        FieldAccessor<TestClass3, String> testClass3StringFieldAccessor =
                new FieldAccessor<TestClass3, String>(TestClass3.class, field);
        TestClass3 testClass3 = new TestClass3();
        testClass3StringFieldAccessor.setValue(testClass3, "foo");
        assertEquals("foo", testClass3StringFieldAccessor.getValue(testClass3));
    }

    @Test
    public void testSetFieldClass4() throws NoSuchMethodException {
        Field field = ReflectionUtils.getField(TestClass4.class, "field");
        FieldAccessor<TestClass4, String> testClass4StringFieldAccessor =
                new FieldAccessor<TestClass4, String>(TestClass4.class, field);
        TestClass4 testClass4 = new TestClass4();
        testClass4StringFieldAccessor.setValue(testClass4, "foo");
        assertEquals("foo", testClass4StringFieldAccessor.getValue(testClass4));
    }

}
