package com.fortumo.ws;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SumServiceTest {

    /**
     * Tests {@link SumService#doEnd()} method when 100 threads waiting for {@link SumService#doEnd()}.
     */
    @Test
    void whenDoEndThenGetSumFromAllThreads() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newCachedThreadPool();

        int threadCount = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);

        SumService service = service(latch::countDown);

        double expectedValue = 0;

        List<Future<Double>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            int fi = i;
            expectedValue += fi;
            futures.add(executor.submit(() -> service.doAdd(fi)));
        }

        latch.await();

        double actualValue = service.doEnd();
        assertEquals(expectedValue, actualValue);
        for (Future<Double> future : futures) {
            assertEquals(expectedValue, future.get());
        }

        executor.shutdown();
    }

    /**
     * Tests {@link SumService#doEnd()} is called before {@link SumService#doAdd(double)},
     * in which case, the <tt>doEnd()</tt> method will return immediately with zero and <tt>doAdd()</tt> will have
     * to wait for another <tt>doEnd()</tt>.
     */
    @Test
    void whenDoEndCalledBeforeDoAddThenGetValue() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        CountDownLatch latch = new CountDownLatch(1);

        SumService service = service(latch::countDown);
        assertEquals(0, service.doEnd());

        double expectedValue = 10;
        Future<Double> actualFuture = executor.submit(() -> service.doAdd(expectedValue));
        latch.await();
        assertEquals(expectedValue, service.doEnd());
        assertEquals(expectedValue, actualFuture.get());

        executor.shutdown();
    }

    /**
     * Tests service re-usability after {@link SumService#doEnd()} is called. Also tests {@link IMonitor#doWait(long)}
     * method to increase the coverage.
     */
    @Test
    void testReuseOnComplete() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(1);

        IMonitor monitor = new IMonitor() {
        };
        SumService service = service(monitor::doNotify);

        Future<Double> actualFuture;
        double expectedValue = 10;

        actualFuture = executor.submit(() -> service.doAdd(expectedValue));
        monitor.doWait(0);
        service.doEnd();
        assertEquals(expectedValue, actualFuture.get());

        actualFuture = executor.submit(() -> service.doAdd(expectedValue));
        monitor.doWait(0);
        service.doEnd();
        assertEquals(expectedValue, actualFuture.get());

        executor.shutdown();
    }

    /**
     * We will have 3 waiting threads. One of them will fail while waiting. Others shall succeed.
     */
    @Test
    void whenErrorOnWaitingThreadThenOthersNotAffected() throws InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();

        int threadCount = 3;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger index = new AtomicInteger(0);
        UnsupportedOperationException expectedException = new UnsupportedOperationException();
        SumService service = service(() -> {
            latch.countDown();
            if (index.incrementAndGet() == 1) {
                throw expectedException;
            }
        });

        List<Future<Double>> futures = new ArrayList<>();
        double numberToAdd = 10;
        double expectedValue = 0;
        for (int i = 0; i < threadCount; i++) {
            expectedValue += numberToAdd;
            futures.add(executor.submit(() -> service.doAdd(numberToAdd)));
        }

        latch.await();
        assertEquals(expectedValue, service.doEnd());

        double actualErrCnt = 0;
        for (Future<Double> future : futures) {
            try {
                assertEquals(expectedValue, future.get());
            } catch (ExecutionException expected) {
                actualErrCnt++;
                assertEquals(expectedException, expected.getCause());
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
            public synchronized void doWait() throws InterruptedException {
                onWait.run();
                super.doWait();
            }
        };
    }
}