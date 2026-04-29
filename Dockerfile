# Stage 1: Build — Maven 3.8 with OpenJDK 11 (full JDK for compilation)
FROM maven:3.8.6-openjdk-11 AS build
WORKDIR /app

# Copy POM first to cache dependency downloads separately from source changes
COPY pom.xml .

# Download all dependencies offline to leverage Docker layer caching
RUN mvn dependency:go-offline -B

# Copy application source code
COPY src ./src

# Build the WAR artifact (skip tests for faster image builds)
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime — Tomcat 9.0 with OpenJDK 11 JRE slim (lightweight, no compiler)
FROM tomcat:9.0.65-jre11-openjdk-slim

# Remove default Tomcat webapps to keep the image clean
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy the built WAR from the build stage into Tomcat's webapps directory
COPY --from=build /app/target/transaction-analyzer.war /usr/local/tomcat/webapps/transaction-analyzer.war

# Set user home to /app/data so H2 database files are written to a mountable volume
ENV JAVA_OPTS="-Duser.home=/app/data"
RUN mkdir -p /app/data

# Expose Tomcat's default HTTP port
EXPOSE 8080

# Start Tomcat in the foreground
CMD ["catalina.sh", "run"]
