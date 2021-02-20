package com.fortumo.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

class SumService implements Function<String, Long> {

    private static final Logger LOG = LoggerFactory.getLogger(SumService.class);

    private final AtomicLong totalSum = new AtomicLong(0);
    private final Map<BlockingLongSupplier, byte[]> suppliers = new ConcurrentHashMap<>();

    @Override
    public Long apply(String request) throws NumberFormatException, ServiceException {
        Objects.requireNonNull(request, "request must not be null");
        try {
            if (request.equals("end")) {
                long sum = totalSum.get();
                totalSum.set(0);
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

    private BlockingLongSupplier addNewSupplier() {
        BlockingLongSupplier supplier = new BlockingLongSupplier();
        suppliers.put(supplier, new byte[0]);
        return supplier;
    }

    private void callAndRemoveSuppliers(Consumer<BlockingLongSupplier> supplierFunction) {
        for (BlockingLongSupplier supplier : suppliers.keySet()) {
            supplierFunction.accept(supplier);
            suppliers.remove(supplier);
        }
    }
}
