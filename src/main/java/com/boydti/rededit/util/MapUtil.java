package com.boydti.rededit.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.TimeUnit;

public class MapUtil {
    public static <T,V> LoadingCache<T,V> getExpiringMap(long time, TimeUnit unit) {
        return CacheBuilder.newBuilder()
                .concurrencyLevel(1)
                .expireAfterWrite(time, unit)
                .build(new CacheLoader() {
                    public Object load(Object key) {
                        throw new RuntimeException();
                    }
                });
    }
}
