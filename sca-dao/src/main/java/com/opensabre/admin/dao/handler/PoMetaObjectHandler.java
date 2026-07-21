package com.opensabre.admin.dao.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.opensabre.admin.common.util.UsernameContext;
import com.opensabre.admin.dao.entity.po.BasePo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.reflection.MetaObject;

import java.time.Instant;
import java.util.Date;

/**
 * MyBatis-Plus审计字段自动填充处理器
 */
@Slf4j
public class PoMetaObjectHandler implements MetaObjectHandler {

    private String getCurrentUsername() {
        String username = UsernameContext.getUsername();
        return StringUtils.defaultIfBlank(username, BasePo.DEFAULT_USERNAME);
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdBy", String.class, getCurrentUsername());
        this.strictInsertFill(metaObject, "createdTime", Date.class, Date.from(Instant.now()));
        this.updateFill(metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedBy", String.class, getCurrentUsername());
        this.strictUpdateFill(metaObject, "updatedTime", Date.class, Date.from(Instant.now()));
    }
}
