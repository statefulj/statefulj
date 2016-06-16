package org.statefulj.framework.tests.clients;

import org.springframework.stereotype.Component;
import org.statefulj.framework.core.annotations.FSM;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.framework.tests.model.User;

import javax.inject.Inject;

/**
 * Created by andrewhall on 8/21/15.
 */
@Component
public class FSMClient1 {

    public StatefulFSM<User> userStatefulFSM;

    @Inject
    public FSMClient1(@FSM("userController") StatefulFSM<User> userStatefulFSM) {
        this.userStatefulFSM = userStatefulFSM;
    }

}
