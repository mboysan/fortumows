package com.fortumo.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The service that collects and sums up the numbers received from the clients and waits until one of the client sends
 * the 'end' signal. This service is represented as a {@link Function} in order to easily understand the class
 * signature, i.e. the expected type of request and the type of response it returns.
 */
class SumService implements Function<String, Long> {

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
     * Processes the given <tt>request</tt> string. If the <tt>request</tt> is a number, the number will be added to
     * the {@link #totalSum} and then will block until it receives the 'end' signal.
     * @param request the request to process.
     * @return the sum of all the numbers provided by separate requests.
     * @throws NumberFormatException if the request cannot be parsed to a valid {@link Long}.
     * @throws ServiceException in case of any unexpected exception.
     */
    @Override
    public Long apply(String request) throws NumberFormatException, ServiceException {
        Objects.requireNonNull(request, "request must not be null");
        try {
            if (request.equals("end")) {
                long sum = totalSum.getAndSet(0);
                LOG.info("notifying suppliers with sum={}", sum);
                callAndRemoveSuppliers(supplier -> supplier.onComplete(sum));   // notifies
                return sum;
            } else {
                long number = Long.parseLong(request);
                BlockingLongSupplier supplier = addNewSupplier();
                long currentSum = totalSum.addAndGet(number);
                LOG.info("added new supplier (currentSum={}), waiting...", currentSum);
                return supplier.getAsLong();    // blocks
            }
        } catch (NumberFormatException e) {
            LOG.error(e.getMessage());
            throw e;
        } catch (Exception e) {
            LOG.error(e.getMessage());
            callAndRemoveSuppliers(supplier -> supplier.onError(e));
            throw new ServiceException(e);

        }
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
    private void callAndRemoveSuppliers(Consumer<BlockingLongSupplier> supplierFunction) {
        for (BlockingLongSupplier supplier : suppliers.keySet()) {
            supplierFunction.accept(supplier);
            suppliers.remove(supplier);
        }
    }
}
