FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

# 先複製 Maven 設定以利用快取
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw mvnw
COPY mvnw.cmd mvnw.cmd
RUN mvn -q -DskipTests dependency:go-offline

# 再複製專案原始碼
COPY src src

# 編譯打包
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
ENV TZ=Asia/Taipei
COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
