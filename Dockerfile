# ==========================================
# GIAI ĐOẠN 1: TẢI THƯ VIỆN VÀ ĐÓNG GÓI CODE
# ==========================================
# ĐÃ SỬA: Đổi sang Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy file cấu hình thư viện và toàn bộ code vào
COPY pom.xml .
COPY src ./src

# Chạy lệnh build (bỏ qua test)
RUN mvn clean package -DskipTests

# ==========================================
# GIAI ĐOẠN 2: KHỞI CHẠY DỰ ÁN
# ==========================================
# ĐÃ SỬA: Đổi sang Java 21
FROM eclipse-temurin:21-jre
WORKDIR /app

# Lấy file .jar đã đóng gói ở Giai đoạn 1 đưa sang đây
COPY --from=build /app/target/*.jar app.jar

# Khai báo port 8080
EXPOSE 8080

# Lệnh khởi động Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]