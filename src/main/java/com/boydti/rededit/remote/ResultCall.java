package com.boydti.rededit.remote;

import com.boydti.fawe.object.RunnableVal2;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ResultCall<T> extends RunnableVal2<Server, T> {

    private ConcurrentHashMap<Server, T> results = new ConcurrentHashMap<>();
    private AtomicInteger returned = new AtomicInteger();
    private AtomicInteger added = new AtomicInteger();

    private int targetMin;
    private int targetMax;

    @Override
    public final void run(Server server, T result) {
        int sizeA = Integer.MIN_VALUE;
        if (add(server, result)) {
            sizeA = added.incrementAndGet();
        }
        int sizeR = returned.incrementAndGet();
        if (sizeR >= targetMax || sizeA >= targetMin) {
            synchronized (this) {
                this.notifyAll();
            }
        }
    }

    public boolean add(Server server, T result) {
        results.put(server, result);
        return true;
    }

    public final void setNumResults(int min, int max) {
        this.targetMin = min;
        this.targetMax = max;
    }

    public final ConcurrentHashMap<Server, T> getResults() {
        return results;
    }
}
