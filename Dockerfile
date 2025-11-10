# 构建阶段
FROM maven:3.8.8 AS builder
WORKDIR /workspace
COPY pom.xml .
# 利用缓存，如果pom.xml没变就不重新下载依赖
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# 运行阶段
FROM openjdk:17.0.2-jdk-slim
WORKDIR /app
# 从构建阶段复制jar包
COPY --from=builder /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]