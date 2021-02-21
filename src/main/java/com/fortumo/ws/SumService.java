package com.fortumo.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The service that collects and sums up the numbers (handled by {@link #doAdd(long)}) received from the clients and
 * waits until one of the client sends the 'end' signal (handled by {@link #doEnd()}).
 * <br><br>
 * Implementation Notes: <br>
 * In most cases, the clients which called {@link #doAdd(long)} shall block until {@link #doEnd()} method is called
 * and the latest value of the {@link #totalSum} is reflected to all waiting threads.
 * In rare cases however, following scenario might happen: <br>
 * Assume there are two threads (t1, t2) and following executions happen one after the other:
 * <ol>
 *     <li>t1 passed point [3] and totalSum is set to 0</li>
 *     <li>t2 passes point [1] and gets currentSum as zero + number</li>
 *     <li>t1 acts fast and reaches [4] before t1 reaches [2]. onComplete() is called before getAsLong().</li>
 *     <li>t2 returns immediately with previous totalSum.</li>
 *     <li>t1 returns as well with previous totalSum.</li>
 * </ol>
 * In this case, number at point [1] is ignored and a separate {@link #doEnd()} call is needed to get the value
 * supplied at that point.
 * TODO: evolve the current implementation to cover the scenario described above.
 */
class SumService {

    private static final Logger LOG = LoggerFactory.getLogger(SumService.class);

    /**
     * The sum of all the numbers received from the clients.
     */
    private final AtomicLong totalSum = new AtomicLong(0);

    /**
     * Supplier acting like a barrier between different threads.
     */
    private final BlockingLongSupplier blockingLongSupplier = new BlockingLongSupplier();

    /**
     * Adds the provided <tt>number</tt> to {@link #totalSum} and waits for {@link #doEnd()} call.
     * @param number the number to add.
     * @return the {@link #totalSum}.
     * @throws ServiceException in case there is any error wih the created {@link BlockingLongSupplier}.
     */
    long doAdd(long number) throws ServiceException {
        long currentSum = totalSum.addAndGet(number);   // [1]
        LOG.info("added new value={}, currentSum={}, waiting...", number, currentSum);
        return blockingLongSupplier.getAsLong(); // [2] - blocks
    }

    /**
     * Notifies all the waiting clients that the 'end' signal is received and then sets the {@link #totalSum} to zero.
     * @return the {@link #totalSum}.
     */
    long doEnd() throws ServiceException {
        long sum = totalSum.getAndSet(0);   // [3]
        LOG.info("notifying suppliers with sum={}", sum);
        blockingLongSupplier.onComplete(sum);   // [4] - notify and wait others to collect
        return sum;
    }
}
