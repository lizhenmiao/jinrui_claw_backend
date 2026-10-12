```bash
项目简介
这是一个基于 Java / Spring Boot 的最小可用后台系统，主要用于支撑“小龙虾 U盘版”的商品化管理。
主要功能模块：
授权码管理：创建授权、启用/禁用、设置到期时间、绑定 U 盘 ID。
设备记录：记录启动电脑、系统版本、U 盘文件系统、最近启动时间等。
版本管理：配置最新版本、下载链接、更新说明、是否强制更新。
模型管理：配置内置模型、Base URL、API Key、默认模型。
事件日志：记录客户端上报的启动失败、微信超时、QQ 离线、模型认证失败等售后事件。
简单后台页面：访问 /admin/index.html。
🚀 运行与访问
启动命令：
bash

编辑



cd xlx-admin-java
mvn spring-boot:run
默认地址：http://127.0.0.1:15000/admin/index.html
默认账号：
用户名：admin
密码：admin123456
环境变量配置：可以通过设置 XLX_ADMIN_USERNAME、XLX_ADMIN_PASSWORD 等环境变量来修改默认配置。
🛠️ 默认数据与接口
演示数据：首次启动会自动创建演示授权码（XLX-DEMO-0001）、演示版本（2026.7.24）和演示内置模型。
客户端接口：包括授权检查、设备心跳、更新检查、模型配置和事件上报。
管理端接口：需先通过 /api/admin/auth/login 登录获取 JWT，后续请求需携带 Authorization: Bearer <accessToken>。
🗄️ 数据库配置
默认连接：本地 MySQL (jdbc:mysql://127.0.0.1:3306/xlx_admin)。
初始化：启动前需创建数据库，或直接执行项目中的 mysql-init.sql。
默认连接参数：
用户：root
密码：空
临时测试：支持使用 H2 数据库，启动时添加 --spring.profiles.active=h2 参数即可。java -jar target/xlx-admin-java-0.1.0.jar --spring.profiles.active=h2
```
