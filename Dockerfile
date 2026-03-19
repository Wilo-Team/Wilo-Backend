# Multi-stage build for optimized image size
FROM eclipse-temurin:21-jre-alpine AS runtime

# 작업 디렉토리 설정
WORKDIR /app

# JAR 파일 복사
COPY build/libs/*.jar app.jar

# 타임존 설정
ENV TZ=Asia/Seoul
RUN apk add --no-cache tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]