package com.opensabre.admin.web.sentinel;

import com.opensabre.admin.common.message.BaseMessage;
import com.opensabre.admin.common.message.EventType;
import com.opensabre.admin.common.message.SentinelBlockMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Sentinel 拦截审计事件发布器
 * <p>
 * BlockAuditSlot 由 Sentinel SPI 加载，不是 Spring Bean，无法直接注入 KafkaTemplate，
 * 因此本类通过静态实例桥接：Spring 启动后将自身注册到静态引用，Slot 通过静态方法投递事件。
 * <p>
 * 投递链路：Slot 线程 offer 有界队列（非阻塞，满则丢弃）→ 后台守护线程 take → KafkaTemplate 发送。
 * Kafka 不可用、队列满、Bean 未就绪等场景一律 fail-open，只记日志，绝不影响业务请求。
 */
@Slf4j
@Component
public class BlockAuditEventPublisher {

    /** 有界队列容量，防止 Kafka 长时间不可用时内存膨胀 */
    private static final int QUEUE_CAPACITY = 1024;

    /** 静态实例桥接，供非 Spring 管理的 BlockAuditSlot 调用 */
    private static volatile BlockAuditEventPublisher instance;

    private final LinkedBlockingQueue<SentinelBlockMessage> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    private volatile boolean running = true;

    @Value("${spring.application.name:sca-web}")
    private String appName;

    @Value("${kafka.topic.sentinel-block:topic-sentinel-block}")
    private String sentinelBlockTopic;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Slot 调用入口：非阻塞投递，Bean 未就绪或队列满时丢弃事件
     */
    public static void publishBlockEvent(SentinelBlockMessage event) {
        BlockAuditEventPublisher publisher = instance;
        if (publisher == null) {
            log.debug("[Sentinel][BlockAudit] 发布器未就绪，丢弃审计事件: resource={}", event.getResource());
            return;
        }
        if (!publisher.queue.offer(event)) {
            log.warn("[Sentinel][BlockAudit] 审计队列已满，丢弃事件: resource={}, blockType={}",
                    event.getResource(), event.getBlockType());
        }
    }

    @PostConstruct
    public void init() {
        instance = this;
        Thread sender = new Thread(this::drainLoop, "sentinel-block-audit-sender");
        sender.setDaemon(true);
        sender.start();
        log.info("[Sentinel][BlockAudit] 审计事件发布器已启动: topic={}, queueCapacity={}",
                sentinelBlockTopic, QUEUE_CAPACITY);
    }

    @PreDestroy
    public void destroy() {
        running = false;
        instance = null;
    }

    /**
     * 后台守护线程循环：从队列取事件，包装统一消息信封后发送 Kafka
     */
    private void drainLoop() {
        while (running) {
            try {
                SentinelBlockMessage event = queue.poll(1, TimeUnit.SECONDS);
                if (event == null) {
                    continue;
                }
                event.setApp(appName);
                BaseMessage<SentinelBlockMessage> message = new BaseMessage<>(
                        UUID.randomUUID().toString().replace("-", ""),
                        System.currentTimeMillis(),
                        EventType.SENTINEL_BLOCK.getCode(),
                        event);
                kafkaTemplate.send(sentinelBlockTopic, event.getResource(), message)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.warn("[Sentinel][BlockAudit] Kafka 发送失败: resource={}, error={}",
                                        event.getResource(), ex.getMessage());
                            }
                        });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                // 单条事件失败不退出循环
                log.warn("[Sentinel][BlockAudit] 审计事件发送异常: {}", t.getMessage());
            }
        }
    }
}
