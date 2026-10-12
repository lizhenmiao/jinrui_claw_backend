FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms128m -Xmx512m"
COPY --from=build /src/target/xlx-admin-java-mybatisplus-0.1.1.jar /app/app.jar
VOLUME ["/app/data"]
EXPOSE 15000
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
