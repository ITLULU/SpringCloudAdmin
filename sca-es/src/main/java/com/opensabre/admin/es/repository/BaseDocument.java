package com.opensabre.admin.es.repository;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * ES文档基类 - 所有ES索引文档继承此类
 */
@Data
public abstract class BaseDocument implements Serializable {

    @Id
    private String id;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
