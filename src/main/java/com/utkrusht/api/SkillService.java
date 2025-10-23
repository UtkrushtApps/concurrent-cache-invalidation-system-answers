package com.utkrusht.api;

import com.utkrusht.caching.CacheManager;
import com.utkrusht.notification.NotificationService;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SkillService {
    private static final Logger logger = Logger.getLogger(SkillService.class.getName());

    private final String instanceId;
    private final CacheManager cacheManager;
    private final NotificationService notificationService;

    public SkillService(String instanceId, CacheManager cacheManager, NotificationService notificationService) {
        this.instanceId = instanceId;
        this.cacheManager = cacheManager;
        this.notificationService = notificationService;
    }

    // Simulate fetching skill data
    public String getUserSkill(String userId) {
        String fromCache = cacheManager.get(userId);
        if (fromCache != null) {
            logger.fine("SkillService-" + instanceId + ": Satisfied userId=" + userId + " from cache");
            return fromCache;
        } else {
            // Simulate DB lookup:
            String dbData = "SkillData_for_" + userId;
            cacheManager.put(userId, dbData);
            logger.fine("SkillService-" + instanceId + ": Loaded from DB for userId=" + userId);
            return dbData;
        }
    }

    // Called to update skill for a user (from a client request)
    public void updateUserSkill(String userId, String newSkillData) {
        try {
            // Write to DB would happen here (simulated)
            cacheManager.put(userId, newSkillData);
            logger.info("SkillService-" + instanceId + ": Updated local cache for userId=" + userId);
            // Asynchronously notify all instances to invalidate
            notificationService.dispatchNotification(userId);
            logger.info("SkillService-" + instanceId + ": Dispatched invalidation notification for userId=" + userId);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "SkillService-" + instanceId + ": Failed to update skill for userId=" + userId, ex);
        }
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public String getInstanceId() {
        return instanceId;
    }
}
