package com.opensabre.admin.dao.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.IllegalSQLInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.opensabre.admin.dao.handler.PoMetaObjectHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.mybatis.spring.annotation.MapperScan;

/**
 * MyBatis-Plus自动配置
 */
@AutoConfiguration
@EnableTransactionManagement
@MapperScan("com.opensabre.admin.dao.mapper")
public class MybatisConfig {

    /**
     * 审计字段自动填充
     */
    @Bean
    public PoMetaObjectHandler poMetaObjectHandler() {
        return new PoMetaObjectHandler();
    }

    /**
     * MyBatis-Plus插件配置
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 防止全表更新与删除
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        // SQL性能规范插件
//        interceptor.addInnerInterceptor(new IllegalSQLInnerInterceptor());

        return interceptor;
    }
}
