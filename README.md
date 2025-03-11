# 简介
短链接系统服务端，整体采用Saas方式开发。
# 技术架构
JDK17 + SpringBoot3&SpringCloud 微服务架构。
核心技术:SpringBoot + SpringCloudAlibaba + ShardingSphere + Redis + MySQL + Sentinel
# 项目模块
- 网关服务：服务请求的分发和鉴权。
- 用户服务：用户注册、登录以及查询个人短信息。
- 分组服务：短链接分组的增删改查。
- 短链服务：短链的创建、修改、归档、删除以及访问监控等。
# 项目亮点
- 高并发：通过将跳转短链接放在存储在redis中应对大量用户同时访问的情况
- 海量存储：通过分表应对mysql存储大量数据的问题，使用redis的HyperLogLog和 bitmaps 进行监控数据的存储，减少内存占用。
- 缓存一致性：当短链接有修改时通过删除缓存确保缓存和mysql的一致性。
- 缓存击穿&穿透：通过布隆过滤器、缓存空值以及分布式锁解决缓存击穿和穿透问题。
- 消息队列：使用Redis Stream 构建消息队列，完成大量访问短链接场景下的监控信息存储。
- 系统限流：使用Sentinel 进行接口访问的 QPS 限流。