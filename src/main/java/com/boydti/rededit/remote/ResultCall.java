package com.boydti.rededit.remote;

import com.boydti.fawe.object.RunnableVal2;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ResultCall<T> extends RunnableVal2<Server, T> {

    private ConcurrentHashMap<Server, T> results = new ConcurrentHashMap<>();
    private AtomicInteger returned = new AtomicInteger();

    private int target;

    @Override
    public final void run(Server server, T result) {
        add(server, result);
        int size = returned.incrementAndGet();
        if (size >= target) {
            synchronized (this) {
                this.notifyAll();
            }
        }
    }

    public void add(Server server, T result) {
        results.put(server, result);
    }

    public final void setNumResults(int target) {
        this.target = target;
    }

    public final ConcurrentHashMap<Server, T> getResults() {
        return results;
    }
}
