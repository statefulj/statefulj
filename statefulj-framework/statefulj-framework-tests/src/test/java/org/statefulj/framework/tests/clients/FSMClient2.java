package org.statefulj.framework.tests.clients;

import org.springframework.stereotype.Component;
import org.statefulj.framework.core.annotations.FSM;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.framework.tests.model.MemoryObject;
import org.statefulj.framework.tests.model.User;

import javax.inject.Inject;

/**
 * Created by andrewhall on 8/21/15.
 */
@Component
public class FSMClient2 {

    public StatefulFSM<User> userStatefulFSM;

    public StatefulFSM<MemoryObject> memoryObjectStatefulFSM;

    @Inject
    public FSMClient2(@FSM("concurrencyController") StatefulFSM<User> userStatefulFSM, @FSM StatefulFSM<MemoryObject> memoryObjectStatefulFSM) {
        this.userStatefulFSM = userStatefulFSM;
        this.memoryObjectStatefulFSM = memoryObjectStatefulFSM;
    }

}
