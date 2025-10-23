# Solution Steps

1. Analyze the original code and note the deadlock risk: If CacheManager.acquireLock() is held and then NotificationService.notifyAll() is called (and vice versa), circular waiting can occur between threads if locks are nested in different orders.

2. Design the new architecture to remove any direct, nested locking between cache and notification logic. Decouple their interaction points.

3. Implement CacheManager using ConcurrentHashMap for thread safety and avoid explicit locks for cache operations.

4. Design NotificationService to maintain a thread-safe BlockingQueue for each service instance, using a ConcurrentHashMap to track queues. Notification publishing should only enqueue messages, never hold locks or call back into CacheManager directly.

5. Setup a configurable ExecutorService thread pool in NotificationService so each service instance has a background worker that asynchronously processes its notification queue and invalidates its own cache as needed.

6. On cache update (via SkillService), put the updated data in the local cache, then notify all microservice nodes by fanout: enqueue a notification for each instance's queue using NotificationService.

7. In test code, simulate 5 service instances (each with its own cache), and fire at least 50 concurrent cache updates by picking instances and users at random.

8. Add logging to cache invalidate, notification enqueue, and notification worker steps for clarity on asynchronicity and error paths. Ensure logs make the decoupling clear.

9. Verify in the test that: (a) all notifications are eventually processed (no dropped or stuck notifications, unless queue is full), (b) there are no deadlocks or thread starvation, (c) after all updates, no old/stale cached data remain.

10. Document the configuration points: thread pool size and queue capacity, and demonstrate via test code that the system runs responsively under concurrent load with detailed logging.

