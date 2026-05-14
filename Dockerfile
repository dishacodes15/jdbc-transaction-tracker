# syntax=docker/dockerfile:1
#
# Pragati Bank — Banking Transaction Analyzer
# Multi-stage Docker build: Maven build → Tomcat 9 runtime
#

# ---- Stage 1: Build with Maven + JDK 11 ----
FROM maven:3.8.6-openjdk-11 AS build
WORKDIR /app

# Cache dependency resolution as a separate layer
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copy sources and build the WAR
COPY src ./src
RUN mvn -B clean package

# ---- Stage 2: Tomcat 9 runtime on JRE 11 ----
FROM tomcat:9.0.65-jre11-openjdk-slim

# Clean default webapps so only our app is served
RUN rm -rf /usr/local/tomcat/webapps/* && mkdir -p /app/data

# Copy the WAR. Maven produces ${finalName}.war = transaction-analyzer-1.0-SNAPSHOT.war.
# Deploy it under the /transaction-analyzer context so URLs match the docs.
COPY --from=build /app/target/transaction-analyzer-*.war /usr/local/tomcat/webapps/transaction-analyzer.war

# H2 writes its file DB to ~ (user home). Point that at a mountable volume so
# data persists across container restarts.
ENV JAVA_OPTS="-Duser.home=/app/data"
VOLUME ["/app/data"]

EXPOSE 8080
CMD ["catalina.sh", "run"]
