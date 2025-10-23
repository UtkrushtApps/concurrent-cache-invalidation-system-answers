package com.utkrusht.notification;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.utkrusht.caching.CacheManager;

public class NotificationService {
    private static final Logger logger = Logger.getLogger(NotificationService.class.getName());

    // Each instance (microservice node) gets its own queue for notifications
    private final Map<String, BlockingQueue<CacheInvalidationNotification>> instanceQueues = new ConcurrentHashMap<>();
    private final Map<String, CacheManager> cacheManagers = new ConcurrentHashMap<>();
    private final ExecutorService executor;
    private final int queueCapacity;

    public NotificationService(int threadPoolSize, int queueCapacity) {
        this.executor = Executors.newFixedThreadPool(threadPoolSize);
        this.queueCapacity = queueCapacity;
    }

    // Register a service instance (simulated)
    public void registerInstance(String instanceId, CacheManager cacheManager) {
        // One queue per instance
        if (!instanceQueues.containsKey(instanceId)) {
            BlockingQueue<CacheInvalidationNotification> queue = new LinkedBlockingQueue<>(queueCapacity);
            instanceQueues.put(instanceId, queue);
            cacheManagers.put(instanceId, cacheManager);
            startWorkerForInstance(instanceId);
            logger.info("NotificationService: Registered instance " + instanceId);
        }
    }

    // Send a notification to all instances (fan-out)
    public void dispatchNotification(String userId) {
        for (String instanceId : instanceQueues.keySet()) {
            BlockingQueue<CacheInvalidationNotification> queue = instanceQueues.get(instanceId);
            boolean offered = queue.offer(new CacheInvalidationNotification(userId));
            if (!offered) {
                logger.warning("NotificationService: Queue full for instance " + instanceId + ". Notification dropped for userId=" + userId);
            } else {
                logger.fine("NotificationService: Notification queued for instance " + instanceId + ", userId=" + userId);
            }
        }
    }

    private void startWorkerForInstance(String instanceId) {
        BlockingQueue<CacheInvalidationNotification> queue = instanceQueues.get(instanceId);
        CacheManager cacheManager = cacheManagers.get(instanceId);
        executor.submit(() -> {
            Thread.currentThread().setName("NotifWorker-" + instanceId);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    CacheInvalidationNotification notif = queue.take();
                    logger.info("NotificationService: Instance " + instanceId + " processing notification userId=" + notif.userId);
                    cacheManager.invalidate(notif.userId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // allow graceful shutdown
                    logger.info("NotificationService: Worker for instance " + instanceId + " interrupted");
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "NotificationService: Error while processing notification for instance " + instanceId, ex);
                }
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warning("NotificationService: Timed out waiting for shutdown.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // For testing and introspection
    public int getNumInstances() {
        return instanceQueues.size();
    }
}

class CacheInvalidationNotification {
    public final String userId;
    public CacheInvalidationNotification(String userId) {
        this.userId = userId;
    }
}
