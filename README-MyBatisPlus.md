# XLX Admin MyBatis-Plus Version

This folder is copied from `xlx-admin-java` and converted from Spring Data JPA to MyBatis-Plus.

## What Changed

- `pom.xml` now uses `mybatis-plus-spring-boot3-starter`.
- JPA annotations were removed from entity classes.
- Entity classes now use MyBatis-Plus annotations such as `@TableName`, `@TableId`, and `@TableField`.
- Repository interfaces are now MyBatis-Plus mapper interfaces based on `BaseMapper`.
- `/api/admin/devices` now uses SQL aggregation:
  - grouped by `license_id + usb_id + hostname`
  - joins `licenses` to show `license_key`
  - returns `rawCount` to show how many original rows were merged
- The old JPA auto table update is removed.

## Important

MyBatis-Plus does not auto-create or auto-update tables like the old JPA setting:

```yaml
spring.jpa.hibernate.ddl-auto: update
```

So before first run on a new database, run:

```bash
mysql -uroot -p123456 < mysql-init.sql
```

The current local database `xlx_admin` can keep using the existing tables.

## Run

```bash
cd xlx-admin-java-mybatisplus
mvn spring-boot:run
```

Or build:

```bash
mvn -DskipTests package
java -jar target/xlx-admin-java-mybatisplus-0.1.0.jar
```

Default backend page:

```text
http://127.0.0.1:15000/admin/index.html
```

Default login:

```text
username: admin
password: 123456
```

## Database

Default MySQL connection:

```text
jdbc:mysql://127.0.0.1:3306/xlx_admin
username: root
password: 123456
```

You can override it with environment variables:

```bash
set MYSQL_URL=jdbc:mysql://127.0.0.1:3306/xlx_admin?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
set MYSQL_USER=root
set MYSQL_PASSWORD=123456
mvn spring-boot:run
```

