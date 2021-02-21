package com.fortumo.ws;

/**
 * A thread-safe monitor interface that wraps the <tt>wait</tt> and <tt>notify</tt> calls to allow for fine
 * grained testing.
 */
public interface IMonitor {

    /**
     * {@link #wait()} without timeout.
     */
    default void doWait() {
        doWait(-1);
    }

    /**
     * {@link #wait()} with specified timeout.
     * @param timeoutMillis maximum time to wait in milliseconds. Ignored if less than zero.
     */
    default void doWait(long timeoutMillis) {
        synchronized (this) {
            try {
                if (timeoutMillis < 0) {
                    wait();
                } else {
                    wait(timeoutMillis);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // reset interrupt state
                throw new RuntimeException("interrupted while waiting.", e);
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
