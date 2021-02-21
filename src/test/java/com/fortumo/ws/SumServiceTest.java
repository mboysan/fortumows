package com.fortumo.ws;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SumServiceTest {

    /**
     * Tests {@link SumService#doEnd()} method when 100 threads waiting for {@link SumService#doEnd()}.
     */
    @Test
    void whenDoEnd_thenGetSumFromAllThreads() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newCachedThreadPool();

        int threadCount = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);

        SumService service = service(latch::countDown);

        long expectedValue = 0;

        List<Future<Long>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            int fi = i;
            expectedValue += fi;
            futures.add(executor.submit(() -> service.doAdd(fi)));
        }

        latch.await();

        long actualValue = service.doEnd();
        assertEquals(expectedValue, actualValue);
        for (Future<Long> future : futures) {
            assertEquals(expectedValue, future.get());
        }

        executor.shutdown();
    }

    /**
     * Tests {@link SumService#doEnd()} is called before {@link SumService#doAdd(long)},
     * in which case, the <tt>doEnd()</tt> method will return immediately with zero and <tt>doAdd()</tt> will have
     * to wait for another <tt>doEnd()</tt>.
     */
    @Test
    void whenDoEndCalledBeforeDoAdd_thenGetValue() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        CountDownLatch latch = new CountDownLatch(1);

        SumService service = service(latch::countDown);
        assertEquals(0, service.doEnd());

        long expectedValue = 10;
        Future<Long> actualFuture = executor.submit(() -> service.doAdd(expectedValue));
        latch.await();
        assertEquals(expectedValue, service.doEnd());
        assertEquals(expectedValue, actualFuture.get());

        executor.shutdown();
    }

    /**
     * Tests service re-usability after {@link SumService#doEnd()} is called.
     */
    @Test
    void testReuseOnComplete() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(1);

        IMonitor monitor = new IMonitor() {
        };
        SumService service = service(monitor::doNotify);

        Future<Long> actualFuture;
        long expectedValue = 10;

        actualFuture = executor.submit(() -> service.doAdd(expectedValue));
        monitor.doWait();
        service.doEnd();
        assertEquals(expectedValue, actualFuture.get());

        actualFuture = executor.submit(() -> service.doAdd(expectedValue));
        monitor.doWait();
        service.doEnd();
        assertEquals(expectedValue, actualFuture.get());

        executor.shutdown();
    }

    /**
     * We will have 3 waiting threads. One of them will fail while waiting. Others shall succeed.
     */
    @Test
    void whenErrorOnWaitingThread_thenOthersNotAffected() throws InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();

        int threadCount = 3;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger index = new AtomicInteger(0);
        SumService service = service(() -> {
            latch.countDown();
            if (index.incrementAndGet() == 1) {
                throw new RuntimeException("expected");
            }
        });

        List<Future<Long>> futures = new ArrayList<>();
        long numberToAdd = 10;
        long expectedValue = 0;
        for (int i = 0; i < threadCount; i++) {
            expectedValue += numberToAdd;
            futures.add(executor.submit(() -> service.doAdd(numberToAdd)));
        }

        latch.await();
        assertEquals(expectedValue, service.doEnd());

        long actualErrCnt = 0;
        for (Future<Long> future : futures) {
            try {
                assertEquals(expectedValue, future.get());
            } catch (ExecutionException expected) {
                actualErrCnt++;
                assertEquals("expected", expected.getCause().getMessage());
            }
        }
        assertEquals(1, actualErrCnt);  // we are expecting only 1 error to be thrown.

        executor.shutdown();
    }


    /**
     * Creates a fine grained {@link SumService} that allows a custom runnable (i.e. <tt>onWait</tt>) to be
     * executed when the {@link SumService#doWait()} method is called.
     *
     * @param onWait the method to execute when the {@link SumService#doWait()} method is called.
     * @return the {@link SumService} as a spy.
     */
    private SumService service(Runnable onWait) {
        return new SumService() {
            @Override
            public synchronized void doWait() {
                onWait.run();
                super.doWait();
            }
        };
    }
}