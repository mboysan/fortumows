package com.fortumo.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.LongSupplier;

/**
 * An implementation of the {@link LongSupplier} interface. The {@link #getAsLong()} method blocks until one of
 * {@link #onComplete(long)} or {@link #onError(Throwable)} methods are called.
 * This class is thread-safe.
 * <br><br>
 * <b>NB!</b> The object created from this class is not reusable. i.e. after calling the {@link #onComplete(long)}
 * method, the {@link #getAsLong()} will always return the same value supplied. The same is true for
 * {@link #onError(Throwable)}, in which case, the {@link #getAsLong()} will always throw the supplied error.
 * Although there are no checks made to cover such scenarios, it is the caller's responsibility to comply
 * with this contract.
 */
public class BlockingLongSupplier implements LongSupplier, IMonitor {

    private static final Logger LOG = LoggerFactory.getLogger(BlockingLongSupplier.class);

    /**
     * The value to supply.
     */
    private Long value = null;

    /**
     * The error to report, if any.
     */
    private Throwable error = null;

    private int entries;

    /**
     * Returns the {@link #value} provided by another thread that used the {@link #onComplete(long)} method.
     * If {@link #onComplete(long)} or {@link #onError(Throwable)} method is called before this method, this method
     * returns immediately.
     * @return the {@link #value}.
     */
    @Override
    public synchronized long getAsLong() {
        ++entries;
        try {
            while (value == null && error == null) {
                doWait();
            }
            if (error != null) {
                throw new RuntimeException("onError called before this method.", error);
            }
            return value;
        } finally {
            if (--entries == 0) {
                error = null;
                value = null;
                doNotify();
            }
        }
    }

    /**
     * Notifies the waiting thread that it received the value the supplier is supposed to return.
     * @param value the value for the supplier to return.
     */
    synchronized void onComplete(long value) {
        this.value = value;
        doNotifyAll();
        while (entries > 0) {
            doWait();
        }
    }

    /**
     * Notifies the waiting thread that an error has occurred, therefore, signals that there is no need to wait
     * any longer.
     * @param t the exception to report.
     */
    synchronized void onError(Throwable t) {
        LOG.error(t.getMessage());
        this.error = t;
        doNotifyAll();
        while (entries > 0) {
            doWait();
        }
    }
}
