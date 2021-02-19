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
    void testAll() throws ExecutionException, InterruptedException {
        // TODO: fix tests

        SumService ss = new SumService();

        ExecutorService executor = Executors.newCachedThreadPool();

        long totalExpected = 0;
        List<Future<Long>> futures = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            int finalI = i;
            totalExpected += finalI;
            futures.add(executor.submit(() -> {
                Long workerSum = ss.apply(finalI + "");
                LOG.info("workerSum={}", workerSum);
                return workerSum;
            }));
        }
        executor.execute(() -> {
            try {
                Thread.sleep(1000);
                Long actualSum = ss.apply("end");
                LOG.info("awaited, actualSum={}", actualSum);
            } catch (Exception e) {
                LOG.error(e.getMessage());
            }
        });

        for (Future<Long> future : futures) {
            assertEquals(totalExpected, future.get());
        }

        executor.shutdown();
    }

}