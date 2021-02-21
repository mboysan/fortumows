package com.fortumo.ws;

/**
 * A thread-safe monitor interface that wraps the <tt>wait</tt> and <tt>notify</tt> calls to allow for fine
 * grained testing and abstracting away the error handling of such calls.
 */
public interface IMonitor {

    /**
     * {@link #wait()} without timeout.
     */
    default void doWait() throws InterruptedException {
        doWait(-1);
    }

    /**
     * {@link #wait()} with specified timeout.
     * @param timeoutMillis maximum time to wait in milliseconds. Ignored if less than zero.
     */
    default void doWait(long timeoutMillis) throws InterruptedException {
        synchronized (this) {
            try {
                if (timeoutMillis < 0) {
                    wait();
                } else {
                    wait(timeoutMillis);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // reset interrupt state
                throw e;
            }
        }
    }

    /**
     * {@link #notify()} waiting thread.
     */
    default void doNotify() {
        synchronized (this) {
            notify();
        }
    }

    /**
     * {@link #notifyAll()} waiting threads.
     */
    default void doNotifyAll() {
        synchronized (this) {
            notifyAll();
        }
    }
}
