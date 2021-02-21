package com.fortumo.ws;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

class BlockingLongSupplierTest {

    /**
     * Tests {@link BlockingLongSupplier#getAsLong()} method when 100 threads waiting for
     * {@link BlockingLongSupplier#onComplete(long)}.
     */
    @Test
    void whenOnComplete_thenGetValue() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newCachedThreadPool();

        int threadCount = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);

        BlockingLongSupplier supplier = blockingLongSupplier(latch::countDown);

        List<Future<Long>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(supplier::getAsLong));
        }

        latch.await();

        long expectedValue = 10;
        supplier.onComplete(expectedValue);

        for (Future<Long> future : futures) {
            assertEquals(expectedValue, future.get());
        }

        executor.shutdown();

    }

    /**
     * Tests {@link BlockingLongSupplier#getAsLong()} method when 100 threads waiting for
     * {@link BlockingLongSupplier#onError(Throwable)}.
     */
    @Test
    void whenOnError_thenThrowError() throws InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();

        int threadCount = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);

        BlockingLongSupplier supplier = blockingLongSupplier(latch::countDown);

        Map<Integer, Throwable> actualExceptions = new ConcurrentHashMap<>();
        for (int i = 0; i < threadCount; i++) {
            int finalI = i;
            executor.submit(() -> {
                try {
                    supplier.getAsLong();
                } catch (Throwable t) {
                    actualExceptions.put(finalI, t);
                }
            });
        }

        latch.await();

        IOException expectedException = new IOException("expected");
        supplier.onError(expectedException);

        for (Integer key : actualExceptions.keySet()) {
            Throwable actualException = actualExceptions.get(key);
            assertEquals(expectedException, actualException.getCause());
        }

        executor.shutdown();
    }

    /**
     * Tests {@link BlockingLongSupplier#onComplete(long)} is called before {@link BlockingLongSupplier#getAsLong()},
     * in which case, the thread will not block.
     */
    @Test
    void whenOnCompleteCalledBeforeGet_thenGetValue() {
        BlockingLongSupplier supplier = new BlockingLongSupplier();
        supplier.onComplete(10);
        assertEquals(10, supplier.getAsLong());
    }

    /**
     * Tests {@link BlockingLongSupplier#onError(Throwable)} is called before {@link BlockingLongSupplier#getAsLong()},
     * in which case, the thread will not block.
     */
    @Test
    void whenOnErrorCalledBeforeGet_thenThrowsError() {
        BlockingLongSupplier supplier = new BlockingLongSupplier();
        IOException expectedException = new IOException("expected");
        supplier.onError(expectedException);
        try {
            supplier.getAsLong();
            throw new AssertionError();
        } catch (RuntimeException e) {
            assertEquals(expectedException, e.getCause());
        }
    }

    /**
     * Creates a fine grained {@link BlockingLongSupplier} that allows a custom runnable (i.e. <tt>onWait</tt>) to be
     * executed when the {@link BlockingLongSupplier#doWait()} method is called.
     *
     * @param onWait the method to execute when the {@link BlockingLongSupplier#doWait()} method is called.
     * @return the {@link BlockingLongSupplier} as a spy.
     */
    private BlockingLongSupplier blockingLongSupplier(Runnable onWait) {
        BlockingLongSupplier supplier = spy(new BlockingLongSupplier());
        doAnswer(inv -> {
            synchronized (supplier) {
                onWait.run();
                supplier.wait();
            }
            return null;
        }).when(supplier).doWait();
        return supplier;
    }

}