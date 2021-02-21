package com.fortumo.ws;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class SumServiceTest {

    private static final Logger LOG = LoggerFactory.getLogger(SumServiceTest.class);

    @Test
    void testRegularWorkflow() throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();

        SumService ss = new SumService();

        for (int k = 0; k < 10; k++) {
            int totalConcurrentClients = 100;
            long expectedSum = 0;
            List<Future<Long>> futures = new ArrayList<>();
            for (int i = 0; i < totalConcurrentClients; i++) {
                int finalI = i;
                expectedSum += finalI;
                futures.add(executor.submit(() -> ss.doAdd(finalI)));
            }

            futures.add(executor.submit(() -> {
                try {
                    Thread.sleep(1000);
                    long actualSum = ss.doEnd();
                    LOG.info("[TEST] awaited, actualSum={}", actualSum);
                    return actualSum;
                } catch (Exception e) {
                    LOG.error(e.getMessage());
                    throw new RuntimeException(e);
                }
            }));

            for (Future<Long> future : futures) {
                assertEquals(expectedSum, future.get());
            }
        }

        executor.shutdown();
    }
}