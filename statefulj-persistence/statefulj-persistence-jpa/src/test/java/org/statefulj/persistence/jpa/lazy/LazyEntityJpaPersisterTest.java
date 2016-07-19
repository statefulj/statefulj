package org.statefulj.persistence.jpa.lazy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.statefulj.fsm.Persister;
import org.statefulj.fsm.StaleStateException;
import org.statefulj.fsm.model.State;
import org.statefulj.persistence.jpa.Order;
import org.statefulj.persistence.jpa.embedded.EmbeddedOrder;
import org.statefulj.persistence.jpa.model.StatefulEntity;
import org.statefulj.persistence.jpa.utils.UnitTestUtils;

import javax.annotation.Resource;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/applicationContext-JPAPersisterTests.xml"})
public class LazyEntityJpaPersisterTest {

    @Resource
    Persister<LazyOrder> lazyJpaPersister;

    @Resource
    LazyOrderRepository lazyOrderRepo;

    @Resource
    JpaTransactionManager transactionManager;

    @Resource
    State<LazyOrder> stateA;

    @Resource
    State<LazyOrder> stateB;

    @Resource
    State<LazyOrder> stateC;

    @Test
    public void testValidStateChange() throws StaleStateException, NoSuchFieldException, IllegalAccessException {
        //Setup the database
        UnitTestUtils.startTransaction(transactionManager);

        LazyOrder order = new LazyOrder();
        order = lazyOrderRepo.save(order);

        LazyOrder parentOrder = new LazyOrder();
        parentOrder.setOrder(order);
        parentOrder = lazyOrderRepo.save(parentOrder);

        State<LazyOrder> currentState = lazyJpaPersister.getCurrent(order);
        assertEquals(stateA, currentState);

        long parentOrderId = parentOrder.getId();

        UnitTestUtils.commitTransaction(transactionManager);

        //Update state in transaction and verify
        UnitTestUtils.startTransaction(transactionManager);

        LazyOrder savedParentOrder = lazyOrderRepo.findOne(parentOrderId);
        LazyOrder lazyChildOrder = savedParentOrder.getOrder();

        assertEquals(stateA, lazyJpaPersister.getCurrent(lazyChildOrder));

        lazyJpaPersister.setCurrent(lazyChildOrder, stateA, stateB);
        assertEquals(stateB, lazyJpaPersister.getCurrent(lazyChildOrder));

        UnitTestUtils.commitTransaction(transactionManager);

        //Reload in another transaction, verify, and change to next state
        UnitTestUtils.startTransaction(transactionManager);

        savedParentOrder = lazyOrderRepo.findOne(parentOrderId);
        lazyChildOrder = savedParentOrder.getOrder();
        assertEquals(stateB, lazyJpaPersister.getCurrent(lazyChildOrder));
        lazyJpaPersister.setCurrent(lazyChildOrder, stateB, stateC);

        UnitTestUtils.commitTransaction(transactionManager);

        //Reload in yet another transaction, verify and try to modify state directly
        UnitTestUtils.startTransaction(transactionManager);

        savedParentOrder = lazyOrderRepo.findOne(parentOrderId);
        lazyChildOrder = savedParentOrder.getOrder();
        assertEquals(stateC, lazyJpaPersister.getCurrent(lazyChildOrder));

        Field stateField = StatefulEntity.class.getDeclaredField("state");
        stateField.setAccessible(true);
        stateField.set(lazyChildOrder, "stateD");

        UnitTestUtils.commitTransaction(transactionManager);

        //Verify that direct modification of the field does not updates state
        UnitTestUtils.startTransaction(transactionManager);

        savedParentOrder = lazyOrderRepo.findOne(parentOrderId);
        lazyChildOrder = savedParentOrder.getOrder();
        assertEquals(stateC, lazyJpaPersister.getCurrent(lazyChildOrder));

        UnitTestUtils.commitTransaction(transactionManager);
    }

    @Test(expected=StaleStateException.class)
    public void testInvalidStateChange() throws StaleStateException {
        //Setup the database
        UnitTestUtils.startTransaction(transactionManager);

        LazyOrder order = new LazyOrder();
        order = lazyOrderRepo.save(order);

        LazyOrder parentOrder = new LazyOrder();
        parentOrder.setOrder(order);
        parentOrder = lazyOrderRepo.save(parentOrder);

        assertEquals(stateA, lazyJpaPersister.getCurrent(order));

        long parentOrderId = parentOrder.getId();

        UnitTestUtils.commitTransaction(transactionManager);
        UnitTestUtils.startTransaction(transactionManager);

        LazyOrder savedParentOrder = lazyOrderRepo.findOne(parentOrderId);
        LazyOrder lazyChildOrder = savedParentOrder.getOrder();

        assertEquals(stateA, lazyJpaPersister.getCurrent(lazyChildOrder));

        lazyJpaPersister.setCurrent(order, stateB, stateC);
        UnitTestUtils.commitTransaction(transactionManager);
    }
}
