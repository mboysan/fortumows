package com.fortumo.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

class SumService implements Function<String, Long> {

    private static final Logger LOG = LoggerFactory.getLogger(SumService.class);

    private final AtomicLong totalSum = new AtomicLong(0);
    private final Map<SumObserver, byte[]> observerMap = new ConcurrentHashMap<>();

    @Override
    public Long apply(String request) throws NumberFormatException, ServiceException {
        Objects.requireNonNull(request, "request must not be null");
        try {
            if (request.equals("end")) {
                long sum = totalSum.get();
                totalSum.set(0);
                LOG.info("notifying observers with sum={}", sum);
                for (SumObserver so : observerMap.keySet()) {
                    so.onComplete(sum);
                    observerMap.remove(so);
                }
                return sum;
            } else {
                long number = Long.parseLong(request);
                SumObserver so = new SumObserver();
                observerMap.put(so, new byte[0]);
                long currentSum = totalSum.addAndGet(number);
                LOG.info("added new observer (currentSum={}), waiting...", currentSum);
                return so.get();
            }
        } catch (NumberFormatException e) {
            LOG.error(e.getLocalizedMessage());
            throw e;
        } catch (InterruptedException e) {
            LOG.error(e.getLocalizedMessage());
            Thread.currentThread().interrupt();
            throw new ServiceException(e);
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage());
            throw new ServiceException(e);
        }
    }
}
