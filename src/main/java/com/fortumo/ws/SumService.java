package com.fortumo.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The service that collects and sums up the numbers (handled by {@link #doAdd(long)}) received from the clients and
 * waits until one of the client sends the 'end' signal (handled by {@link #doEnd()}).
 * Note that, objects created from this class are re-usable. This class is thread-safe.
 */
class SumService implements IMonitor {

    private static final Logger LOG = LoggerFactory.getLogger(SumService.class);

    /**
     * The sum of all the numbers received from the clients.
     */
    private long totalSum = 0;

    /**
     * the number of threads that called the {@link #doAdd(long)} method.
     */
    private int entries = 0;

    /**
     * A round is defined as all the {@link #doAdd(long)} calls before a thread calls the {@link #doEnd()}.
     * If false, then a round is currently active and any thread that called the {@link #doAdd(long)} has to
     * wait for notification. This variable is used to protect against spurious wake-ups.
     */
    private boolean roundComplete = false;

    /**
     * Adds the provided <tt>number</tt> to {@link #totalSum} and waits for {@link #doEnd()} call.
     * @param number the number to add.
     * @return the {@link #totalSum}.
     */
    synchronized long doAdd(long number) {
        ++entries;
        try {
            totalSum += number;
            LOG.info("adding number={}, currentSum={}", number, totalSum);
            while (!roundComplete) {
                doWait();
            }
            return totalSum;
        } finally {
            if (--entries == 0) {
                doNotify(); // notify the doEnd thread.
            }
        }
    }

    /**
     * Notifies all the waiting clients that the 'end' signal is received and then sets the {@link #totalSum} to zero.
     * Also waits until all threads that called the {@link #doAdd(long)} exited first.
     * @return the {@link #totalSum}.
     */
    synchronized long doEnd() {
        long sum = totalSum;
        LOG.info("notifying all with sum={}", totalSum);
        roundComplete = true;
        doNotifyAll();
        while (entries > 0) {
            doWait();
        }
        roundComplete = false;  // starting a new round.
        totalSum = 0;
        return sum;
    }
}
