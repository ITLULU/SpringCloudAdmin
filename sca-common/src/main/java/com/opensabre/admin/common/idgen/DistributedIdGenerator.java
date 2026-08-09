package com.opensabre.admin.common.idgen;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;

/**
 * 分布式主键ID生成器（34位纯数字字符串，订单号策略）
 *
 * <p>ID 结构（共34位）：</p>
 * <pre>
 * | 酒店ID(7) | 会员尾号(2) | 日期(8, yyyyMMdd) | 毫秒时间戳(13) | 序列号(4) |
 * 示例: 2000014 36 20260809 1767955200123 0042
 * </pre>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>序列号基于 Redis INCR，按毫秒建 key（TTL 3秒），毫秒切换天然归0；
 *       所有实例必须连接同一套 Redis，序列才能全局唯一；</li>
 *   <li>Redis 故障时降级为 4 位随机数（本机内存去重防本机碰撞）；
 *       注意：跨实例的随机碰撞无法完全避免，建议业务表加唯一索引兜底；</li>
 *   <li>时钟回拨治理：小回拨(≤5ms)等待追上；仍回拨则沿用旧时间戳续发（序列仍递增）；
 *       大回拨拒绝发号，防止重复；</li>
 *   <li>日期段固定使用 Asia/Shanghai 时区，避免跨时区机房部署导致日期段不一致。</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>
 * &#64;Bean
 * public DistributedIdGenerator orderIdGenerator(StringRedisTemplate redisTemplate) {
 *     return new DistributedIdGenerator("2000014", redisTemplate);
 * }
 * </pre>
 */
@Slf4j
public class DistributedIdGenerator {

    /** 各段位数定义 */
    public static final int HOTEL_ID_LENGTH = 7;
    public static final int MEMBER_SUFFIX_LENGTH = 2;
    public static final int DATE_LENGTH = 8;
    public static final int TIMESTAMP_LENGTH = 13;
    public static final int SEQUENCE_LENGTH = 4;
    /** ID 总长度 */
    public static final int ID_LENGTH = HOTEL_ID_LENGTH + MEMBER_SUFFIX_LENGTH + DATE_LENGTH
            + TIMESTAMP_LENGTH + SEQUENCE_LENGTH;

    /** 非会员的会员尾号占位 */
    public static final String NON_MEMBER_SUFFIX = "00";

    /** 4位序列号上限：0000 ~ 9999 */
    private static final long MAX_SEQUENCE = 10_000L;
    /** 允许的最大时钟回拨毫秒数，超过则拒绝发号 */
    private static final long MAX_CLOCK_BACKWARD_MS = 5L;
    /** 序列号 key 的 TTL，仅需覆盖所在毫秒即可，3秒足够安全 */
    private static final Duration SEQ_KEY_TTL = Duration.ofSeconds(3);
    /** Redis 序列号 key 前缀 */
    private static final String SEQ_KEY_PREFIX = "sca:idgen:seq:";
    /** 批量取号单次上限（订单主表+明细表一次落库场景） */
    public static final int MAX_BATCH_SIZE = 5000;
    /** 降级随机取号的最大碰撞重试次数 */
    private static final int FALLBACK_RETRY_LIMIT = 1000;
    /** 日期段格式化（固定时区，避免跨时区机房不一致） */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("Asia/Shanghai"));

    /** 酒店ID（7位数字，如 2000014） */
    private final String hotelId;
    /** Redis 模板，可为 null（为 null 时始终走随机降级） */
    private final StringRedisTemplate redisTemplate;

    /** 上次发号时间戳，用于时钟回拨保护（synchronized 保护下读写） */
    private long lastTimestamp = -1L;
    /** Redis 成功发号的最后一个毫秒（本机视角），用于禁止同毫秒内 Redis/随机序列混用 */
    private long redisIssuedMs = -1L;
    /** 随机降级：所属毫秒 */
    private long fallbackMs = -1L;
    /** 随机降级：本毫秒内已发出的序列号集合（本机去重） */
    private final Set<Integer> fallbackIssued = new HashSet<>();

    /**
     * 构造函数
     *
     * @param hotelId       酒店ID，必须为7位数字（如 "2000014"）
     * @param redisTemplate Redis 模板，为 null 时退化为纯随机发号（有跨实例碰撞风险）
     */
    public DistributedIdGenerator(String hotelId, StringRedisTemplate redisTemplate) {
        if (StringUtils.isBlank(hotelId) || !hotelId.matches("\\d{" + HOTEL_ID_LENGTH + "}")) {
            throw new IllegalArgumentException("hotelId 必须为" + HOTEL_ID_LENGTH + "位数字: " + hotelId);
        }
        this.hotelId = hotelId;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 生成下一个34位纯数字ID（非会员场景，会员尾号段为 00），线程安全
     */
    public String nextId() {
        return nextId(null);
    }

    /**
     * 生成下一个34位纯数字ID（线程安全）
     *
     * @param memberId 会员ID，取其末2位作为会员尾号段；为 null/空白或非会员时填 00。
     *                 注意：该段仅为信息标识，不参与唯一性（唯一性由时间戳+序列号保证）
     */
    public synchronized String nextId(String memberId) {
        String memberSuffix = memberSuffix(memberId);
        while (true) {
            long ts = safeTimestamp();
            // 1) 优先 Redis 全局序列
            if (redisTemplate != null) {
                long seq = tryRedisSequence(ts);
                if (seq >= MAX_SEQUENCE) {
                    // 同一毫秒序列溢出（>1万/ms），等待下一毫秒重试
                    log.warn("同毫秒序列号溢出, 等待下一毫秒: ts={}", ts);
                    sleepUntilAfter(ts);
                    continue;
                }
                if (seq >= 0) {
                    redisIssuedMs = ts;
                    return format(memberSuffix, ts, seq);
                }
                // 2) Redis 失败降级：若本毫秒 Redis 已发过号（本机视角），必须先等下一毫秒再随机，
                //    否则随机数可能撞上 Redis 已发出的序列号
                if (redisIssuedMs == ts) {
                    sleepUntilAfter(ts);
                    continue;
                }
            }
            // 3) 随机降级（本机内存去重；跨实例碰撞需业务唯一索引兜底）
            long seq = nextFallbackSequence(ts);
            if (seq < 0) {
                sleepUntilAfter(ts);
                continue;
            }
            return format(memberSuffix, ts, seq);
        }
    }

    /**
     * 批量生成ID（非会员场景），单次上限 {@link #MAX_BATCH_SIZE}
     */
    public List<String> nextIds(int count) {
        return nextIds(null, count);
    }

    /**
     * 批量生成ID（适用于订单主表+明细表一次落库场景），单次上限 {@link #MAX_BATCH_SIZE}
     *
     * @param memberId 会员ID，同 {@link #nextId(String)}
     * @param count    取号数量
     */
    public List<String> nextIds(String memberId, int count) {
        if (count <= 0 || count > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("批量取号数量须在[1, " + MAX_BATCH_SIZE + "]内: " + count);
        }
        List<String> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(nextId(memberId));
        }
        return ids;
    }

    // ==================== 内部实现 ====================

    /**
     * 时钟回拨保护：返回可用于发号的时间戳（不小于 lastTimestamp）
     */
    private long safeTimestamp() {
        long now = System.currentTimeMillis();
        if (now < lastTimestamp) {
            long offset = lastTimestamp - now;
            if (offset > MAX_CLOCK_BACKWARD_MS) {
                throw new IllegalStateException(
                        String.format("时钟回拨 %d ms，超过阈值 %d ms，拒绝发号以防重复", offset, MAX_CLOCK_BACKWARD_MS));
            }
            // 小回拨：等待时钟追上
            LockSupport.parkNanos(offset * 2 * 1_000_000L);
            now = System.currentTimeMillis();
            if (now < lastTimestamp) {
                // 仍未追上：沿用旧时间戳续发，序列号在同毫秒 key 内继续递增，依然唯一
                log.warn("时钟回拨 {} ms，沿用 lastTimestamp={} 继续发号", lastTimestamp - now, lastTimestamp);
                return lastTimestamp;
            }
        }
        lastTimestamp = now;
        return now;
    }

    /**
     * 从 Redis 获取当前毫秒的全局序列号。
     * <p>按毫秒建 key，毫秒切换自动归0；TTL 3秒防止 key 堆积。
     * Redis 重启/主从切换丢失计数时，回放的窗口仅限同一毫秒（远短于重启耗时），不会重复。</p>
     *
     * @return 序列号(1~9999)；Redis 异常时返回 -1 表示降级
     */
    private long tryRedisSequence(long ts) {
        String key = SEQ_KEY_PREFIX + ts;
        try {
            Long seq = redisTemplate.opsForValue().increment(key);
            if (seq == null) {
                return -1L;
            }
            if (seq == 1L) {
                // 首次创建时设置过期时间；expire 失败仅造成少量 key 残留，不影响正确性
                redisTemplate.expire(key, SEQ_KEY_TTL);
            }
            return seq;
        } catch (Exception e) {
            log.error("Redis 获取序列号失败，降级为随机序列: key={}", key, e);
            return -1L;
        }
    }

    /**
     * 随机降级序列：4位随机数 + 本机内存去重。
     * <p>注意：仅能保证本机同毫秒内不重复，跨实例碰撞概率约为 1/10000/对，
     * 需业务表唯一索引兜底。</p>
     *
     * @return 序列号(0~9999)；本毫秒随机空间接近耗尽时返回 -1（等待下一毫秒）
     */
    private long nextFallbackSequence(long ts) {
        if (fallbackMs != ts) {
            fallbackMs = ts;
            fallbackIssued.clear();
        }
        if (fallbackIssued.size() >= MAX_SEQUENCE) {
            return -1L;
        }
        for (int i = 0; i < FALLBACK_RETRY_LIMIT; i++) {
            int seq = ThreadLocalRandom.current().nextInt((int) MAX_SEQUENCE);
            if (fallbackIssued.add(seq)) {
                return seq;
            }
        }
        // 连续碰撞过多（说明空间已高度饱和），放弃本毫秒
        return -1L;
    }

    /** 自旋等待直到系统时间超过 ts */
    private void sleepUntilAfter(long ts) {
        while (System.currentTimeMillis() <= ts) {
            LockSupport.parkNanos(100_000L); // 0.1ms
        }
    }

    /** 提取会员尾号：取 memberId 末2位数字；非会员/空白/无数字时返回 00 */
    private static String memberSuffix(String memberId) {
        if (StringUtils.isBlank(memberId)) {
            return NON_MEMBER_SUFFIX;
        }
        String digits = memberId.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return NON_MEMBER_SUFFIX;
        }
        String tail = digits.length() >= MEMBER_SUFFIX_LENGTH
                ? digits.substring(digits.length() - MEMBER_SUFFIX_LENGTH)
                : digits;
        return pad0(tail, MEMBER_SUFFIX_LENGTH);
    }

    /** 拼装最终34位ID：酒店ID(7) + 会员尾号(2) + 日期(8) + 时间戳(13) + 序列号(4) */
    private String format(String memberSuffix, long ts, long seq) {
        String date = DATE_FORMATTER.format(Instant.ofEpochMilli(ts));
        return hotelId + memberSuffix + date + ts + pad(seq, SEQUENCE_LENGTH);
    }

    /** 字符串左侧补0到指定长度 */
    private static String pad0(String s, int length) {
        if (s.length() >= length) {
            return s;
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = s.length(); i < length; i++) {
            sb.append('0');
        }
        return sb.append(s).toString();
    }

    /** 左侧补0到指定长度 */
    private static String pad(long value, int length) {
        String s = Long.toString(value);
        if (s.length() >= length) {
            return s;
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = s.length(); i < length; i++) {
            sb.append('0');
        }
        return sb.append(s).toString();
    }
}
