package org.statefulj.fsm.model.impl;

/**
 * Created by arnaud33200 on 2017-05-20.
 */
public interface OnStateChangedListener {

    void onEntry();
    void onExit();
}