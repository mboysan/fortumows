package com.fortumo.ws;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class SumServiceTest {

    @Test
    void testAll() throws ExecutionException, InterruptedException {
        // TODO: fix tests

        SumService ss = new SumService();

        ExecutorService executor = Executors.newCachedThreadPool();

        long totalExpected = 0;
        List<Future<Long>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            totalExpected += finalI;
            futures.add(executor.submit(() -> ss.apply(finalI + "")));
        }
        executor.execute(() -> {
            try {
                Thread.sleep(1000);
                System.out.println("awaited and ended total=" + ss.apply("end"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        for (Future<Long> future : futures) {
            assertEquals(totalExpected, future.get());
        }

        executor.shutdown();
    }

}