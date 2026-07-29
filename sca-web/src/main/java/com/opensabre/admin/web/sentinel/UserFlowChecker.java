package com.opensabre.admin.web.sentinel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户维度固定窗口计数器
 * <p>
 * 以 (resource|username) 为 key 维护固定窗口计数：窗口起点按 windowSeconds 对齐，
 * 窗口轮替时 CAS 重置计数，读写全程无锁，适合"分钟级少量次数"的防刷场景
 * （固定窗口的临界突刺最坏 2 倍阈值，对防刷语义可接受）。
 * <p>
 * 内存控制：守护线程每 5 分钟清理一轮过期窗口（窗口起点早于 2 倍窗口长度前的条目）。
 */
public final class UserFlowChecker {

    private static final ConcurrentHashMap<String, WindowCounter> COUNTERS = new ConcurrentHashMap<>();

    /**
     * 过期清理周期（秒）
     */
    private static final long CLEAN_INTERVAL_SECONDS = 300;

    static {
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sentinel-userflow-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleAtFixedRate(UserFlowChecker::cleanExpired,
                CLEAN_INTERVAL_SECONDS, CLEAN_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private UserFlowChecker() {
    }

    /**
     * 尝试通过并计数
     *
     * @return true 放行（窗口内次数未超阈值）；false 拦截
     */
    public static boolean tryPass(String resource, String username, UserFlowRule rule) {
        long windowMillis = rule.getWindowSeconds() * 1000L;
        long windowStart = System.currentTimeMillis() / windowMillis * windowMillis;
        WindowCounter counter = COUNTERS.computeIfAbsent(resource + "|" + username,
                k -> new WindowCounter(windowStart));
        return counter.incrementInWindow(windowStart, windowMillis) <= rule.getCountPerUser();
    }

    /**
     * 清理过期窗口条目（窗口起点早于当前时间 2 个清理周期的视为不活跃）
     */
    private static void cleanExpired() {
        long expireBefore = System.currentTimeMillis() - CLEAN_INTERVAL_SECONDS * 2 * 1000;
        COUNTERS.entrySet().removeIf(e -> e.getValue().windowStart.get() < expireBefore);
    }

    /**
     * 单个 (resource, user) 的固定窗口计数器
     */
    private static class WindowCounter {
        final AtomicLong windowStart;
        final AtomicInteger count = new AtomicInteger(0);

        WindowCounter(long windowStart) {
            this.windowStart = new AtomicLong(windowStart);
        }

        /**
         * 在指定窗口内计数 +1；窗口轮替时 CAS 重置后再计数
         *
         * @return 计数后的窗口内累计次数
         */
        int incrementInWindow(long currentWindowStart, long windowMillis) {
            long recorded = windowStart.get();
            if (recorded != currentWindowStart) {
                // 窗口轮替：仅一个线程 CAS 成功并重置计数，失败方直接沿用新窗口计数
                if (windowStart.compareAndSet(recorded, currentWindowStart)) {
                    count.set(0);
                } else if (windowStart.get() != currentWindowStart) {
                    // 极端并发下已被推进到更新的窗口（时钟回拨等），放行本次
                    return 1;
                }
            }
            return count.incrementAndGet();
        }
    }
}
