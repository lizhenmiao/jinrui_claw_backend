# Docker Run Guide

This project can create the database and tables automatically on startup.

## Auto Database/Table Creation

The application uses:

- `createDatabaseIfNotExist=true` in the MySQL JDBC URL to create `xlx_admin` when it is missing.
- Flyway migration `src/main/resources/db/migration/V1__init_schema.sql` to create tables.
- `spring.flyway.baseline-on-migrate=true` so an existing non-empty database can still start.

The MySQL user must have permission to create databases and tables. For local Docker, the compose file uses `root`.

## Build Jar Locally

```bash
mvn -DskipTests package
java -jar target/xlx-admin-java-mybatisplus-0.1.0.jar
```

## Build Docker Image

```bash
docker build -t xlx-admin-java-mybatisplus:0.1.0 .
```

## Start With MySQL

```bash
docker compose up -d --build
```

Open:

```text
http://127.0.0.1:15000/admin/index.html
```

Default login:

```text
admin / 123456
```

## Stop

```bash
docker compose down
```

Stop and delete database/upload volumes:

```bash
docker compose down -v
```

## Important Environment Variables

```text
MYSQL_URL              MySQL JDBC URL
MYSQL_USER             MySQL user
MYSQL_PASSWORD         MySQL password
XLX_ADMIN_USERNAME     Admin username
XLX_ADMIN_PASSWORD     Admin password
XLX_UPLOAD_DIR         Version package upload directory
XLX_PUBLIC_BASE_URL    Public backend URL used in version package download links
JAVA_OPTS              JVM options, for example -Xms128m -Xmx512m
```

When deploying to a real server, change:

```text
XLX_ADMIN_PASSWORD
MYSQL_PASSWORD
XLX_PUBLIC_BASE_URL
XLX_CRYPTO_SECRET
XLX_JWT_SECRET
```
