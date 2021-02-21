package com.fortumo.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * The service that collects and sums up the numbers (handled by {@link #doAdd(long)}) received from the clients and
 * waits until one of the client sends the 'end' signal (handled by {@link #doEnd()}).
 */
class SumService {

    private static final Logger LOG = LoggerFactory.getLogger(SumService.class);

    /**
     * The sum of all the numbers received from the clients.
     */
    private final AtomicLong totalSum = new AtomicLong(0);
    /**
     * A map of {@link BlockingLongSupplier}s. Each request will be mapped to a separate supplier as the key for this
     * map. Once the 'end' signal is received, the suppliers will be removed.
     */
    private final Map<BlockingLongSupplier, byte[]> suppliers = new ConcurrentHashMap<>();

    /**
     * Adds the provided <tt>number</tt> to {@link #totalSum} and waits for {@link #doEnd()} call.
     * @param number the number to add.
     * @return the {@link #totalSum}.
     * @throws ServiceException in case there is any error wih the created {@link BlockingLongSupplier}.
     */
    long doAdd(long number) throws ServiceException {
        BlockingLongSupplier supplier = null;
        try {
            supplier = addNewSupplier();
            long currentSum = totalSum.addAndGet(number);
            LOG.info("added new supplier (currentSum={}), waiting...", currentSum);
            return supplier.getAsLong();    // blocks
        } catch (Exception e) {
            if (supplier != null) {
                callAndRemoveSupplier(supplier, s -> s.onError(e));
            }
            throw new ServiceException(e);
        }
    }

    /**
     * Notifies all the waiting clients that the 'end' signal is received.
     * @return the {@link #totalSum}.
     */
    long doEnd() throws ServiceException {
        long sum = totalSum.getAndSet(0);
        LOG.info("notifying suppliers with sum={}", sum);
        callAndRemoveAllSuppliers(supplier -> supplier.onComplete(sum));   // notifies
        return sum;
    }

    /**
     * Creates a new {@link BlockingLongSupplier} and adds it to {@link #suppliers}.
     * @return the created {@link BlockingLongSupplier} object.
     */
    private BlockingLongSupplier addNewSupplier() {
        BlockingLongSupplier supplier = new BlockingLongSupplier();
        suppliers.put(supplier, new byte[0]);
        return supplier;
    }

    /**
     * Calls the provided <tt>supplierFunction</tt> for all the {@link #suppliers} and removes them.
     * @param supplierFunction the method of the {@link BlockingLongSupplier} to call.
     */
    private void callAndRemoveAllSuppliers(Consumer<BlockingLongSupplier> supplierFunction) {
        for (BlockingLongSupplier supplier : suppliers.keySet()) {
            callAndRemoveSupplier(supplier, supplierFunction);
        }
    }

    /**
     * Calls the provided <tt>supplierFunction</tt> only for the provided <tt>supplier</tt> and removes it from the
     * {@link #suppliers}.
     * @param supplierFunction the method of the {@link BlockingLongSupplier} to call.
     */
    private void callAndRemoveSupplier(BlockingLongSupplier supplier, Consumer<BlockingLongSupplier> supplierFunction) {
        try {
            supplierFunction.accept(supplier);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        } finally {
            suppliers.remove(supplier);
        }
    }
}
