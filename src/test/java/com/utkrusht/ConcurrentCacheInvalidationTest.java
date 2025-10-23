package com.utkrusht;

import com.utkrusht.caching.CacheManager;
import com.utkrusht.api.SkillService;
import com.utkrusht.notification.NotificationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConcurrentCacheInvalidationTest {
    public static void main(String[] args) throws InterruptedException {
        setupLogging();
        final int NUM_INSTANCES = 5;
        final int NUM_UPDATES = 50;
        final int THREAD_POOL = 4;
        final int QUEUE_CAPACITY = 100;
        final List<SkillService> serviceInstances = new ArrayList<>();
        final NotificationService notificationService = new NotificationService(THREAD_POOL, QUEUE_CAPACITY);
        // Setup instances
        for (int i = 0; i < NUM_INSTANCES; i++) {
            String id = "svc-" + (i + 1);
            CacheManager cm = new CacheManager();
            notificationService.registerInstance(id, cm);
            serviceInstances.add(new SkillService(id, cm, notificationService));
        }
        System.out.println("Registered " + NUM_INSTANCES + " service instances.");
        // Prime caches
        for (SkillService svc : serviceInstances) {
            svc.getUserSkill("u1");
            svc.getUserSkill("u2");
        }
        System.out.println("Primed caches for demo users.");

        final CountDownLatch ready = new CountDownLatch(NUM_UPDATES);
        final List<Thread> threads = new ArrayList<>();
        final Random rnd = new Random();
        System.out.println("Starting " + NUM_UPDATES + " concurrent skill updates...");
        for (int i = 0; i < NUM_UPDATES; i++) {
            final int updateNum = i + 1;
            Thread t = new Thread(() -> {
                SkillService svc = serviceInstances.get(rnd.nextInt(NUM_INSTANCES));
                String userId = "u" + (1 + rnd.nextInt(2)); // either u1 or u2
                String newSkill = "S_NEW_" + updateNum;
                svc.updateUserSkill(userId, newSkill);
                ready.countDown();
            }, "UpdateThread-" + updateNum);
            threads.add(t);
        }
        long start = System.currentTimeMillis();
        for (Thread t : threads) t.start();
        // Wait for all completed
        ready.await(20, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - start;
        System.out.println("All updates dispatched. Elapsed: " + duration + " ms");
        // Give notification workers some time to apply invalidations
        Thread.sleep(2000);
        // Validate: caches should NOT have stale data for u1 and u2
        for (SkillService svc : serviceInstances) {
            for (String userId : new String[]{"u1", "u2"}) {
                String cached = svc.getCacheManager().get(userId);
                if (cached != null && !cached.startsWith("S_NEW_")) {
                    System.err.println("Stale cache found in instance " + svc.getInstanceId() + " for " + userId + ": " + cached);
                }
            }
        }
        notificationService.shutdown();
        System.out.println("Test completed.");
    }

    private static void setupLogging() {
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.INFO);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.INFO);
        rootLogger.addHandler(handler);
    }
}
