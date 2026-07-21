# 主要涉及到本项目依赖工具的部署安装

## docker部署
参考 docker-compose路径下部署 ，直接docker拉取镜像下载

## 安装包部署

因为现在docker 拉取镜像很多都存在问题，下面的依赖工具，就会基于安装包部署

### nacos


### kafka


### ES


### kibana


### seata
镜像配置参考docker-compose


在 Nacos 中导入 Seata 配置

访问 Nacos 控制台 http://服务器IP:8848/nacos，手动创建配置：
Data ID：seataServer.properties
Group：SEATA_GROUP
格式：Properties


配置内容

# 事务组映射（与客户端 application.yml 中保持一致）
service.vgroupMapping.default_tx_group=default

# 存储模式：db
store.mode=db
store.db.datasource=druid
store.db.dbType=mysql
store.db.driverClassName=com.mysql.cj.jdbc.Driver
store.db.url=jdbc:mysql://mysql8:3306/seata?useUnicode=true&rewriteBatchedStatements=true&serverTimezone=Asia/Shanghai
store.db.user=website
store.db.password=website1020
store.db.minConn=5
store.db.maxConn=30
store.db.globalTable=global_table
store.db.branchTable=branch_table
store.db.distributedLockTable=distributed_lock
store.db.queryLimit=100
store.db.lockTable=lock_table
store.db.maxWait=5000

# 事务超时时间（秒）
server.recovery.committingRetryPeriod=1000
server.recovery.asynCommittingRetryPeriod=1000
server.recovery.rollbackingRetryPeriod=1000
server.recovery.timeoutRetryPeriod=1000
server.maxCommitRetryTimeout=-1
server.maxRollbackRetryTimeout=-1
server.rollbackRetryTimeoutUnlockEnable=false

# 事务组默认超时时间（秒）
server.distributedLockExpireTime=10000

### sentinel