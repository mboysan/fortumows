package com.fortumo.ws;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

class SumService implements Function<String, Long> {
    private final AtomicLong totalSum = new AtomicLong(0);
    private final Map<SumObserver, byte[]> observerMap = new ConcurrentHashMap<>();

    @Override
    public Long apply(String request) {
        Objects.requireNonNull(request, "request must not be null");
        try {
            if (request.equals("end")) {
                long sum = totalSum.get();
                totalSum.set(0);
                for (SumObserver so : observerMap.keySet()) {
                    so.onComplete(sum);
                    observerMap.remove(so);
                }
                return sum;
            } else {
                long number = Long.parseLong(request);
                SumObserver so = new SumObserver();
                observerMap.put(so, new byte[0]);
                totalSum.addAndGet(number);
                return so.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        SumService ss = new SumService();

        ExecutorService executor = Executors.newCachedThreadPool();

        List<Future<Long>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            futures.add(executor.submit(() -> ss.apply(finalI + "")));
        }

        System.out.println("current total=" + ss.apply("end"));

        System.out.println("ending futures...");

        executor.execute(() -> {
            try {
                Thread.sleep(1000);
                System.out.println("awaited and ended total=" + ss.apply("end"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        for (Future<Long> future : futures) {
            System.out.println(future.get());
        }

        executor.shutdown();
    }
}
