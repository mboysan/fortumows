package com.fortumo.ws;

class SumObserver {

    private final Sum sum = new Sum();

    SumObserver() {}

    long get() throws InterruptedException {
        synchronized (sum) {
            while (sum.totalSum == null) {  // to protect against Spurious wakeup
                sum.wait();
            }
            long total = sum.totalSum;
            sum.totalSum = null;
            return total;
        }
    }

    void onComplete(long totalSum) {
        synchronized (sum) {
            sum.totalSum = totalSum;
            sum.notify();
        }
    }

    private static class Sum {
        private Long totalSum = null;
    }
}
