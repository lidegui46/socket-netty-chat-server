package com.kingston.im.dispatch;

/**
 * 任务线程
 */
public abstract class DispatchTask implements Runnable {

    protected int dispatchKey;

    public int getDispatchKey() {
        return dispatchKey;
    }

}
