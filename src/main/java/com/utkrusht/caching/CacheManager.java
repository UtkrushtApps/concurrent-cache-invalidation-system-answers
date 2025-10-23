package com.utkrusht.caching;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class CacheManager {
    private static final Logger logger = Logger.getLogger(CacheManager.class.getName());
    // In-memory cache (userId -> skillData)
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
    // For demonstration: track read/write events
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    public void put(String userId, String skillData) {
        cache.put(userId, skillData);
        logger.fine("CacheManager: Put userId=" + userId + ", skillData=" + skillData);
    }

    public String get(String userId) {
        String val = cache.get(userId);
        if (val != null) {
            hits.incrementAndGet();
            logger.fine("CacheManager: Hit for userId=" + userId);
        } else {
            misses.incrementAndGet();
            logger.fine("CacheManager: Miss for userId=" + userId);
        }
        return val;
    }

    public void invalidate(String userId) {
        cache.remove(userId);
        logger.info("CacheManager: Invalidated cache for user " + userId);
    }

    public void clearAll() {
        cache.clear();
        logger.info("CacheManager: Cleared all caches.");
    }

    public long getHits() {
        return hits.get();
    }

    public long getMisses() {
        return misses.get();
    }

    public int size() {
        return cache.size();
    }
}
