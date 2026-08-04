# Build stage
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn package -DskipTests

# Run stage
FROM tomcat:9.0-jdk17-openjdk-slim
LABEL maintainer="neelanshkhare"

# Generate self-signed certificate for HTTPS
RUN keytool -genkey -noprompt \
    -alias tomcat \
    -keyalg RSA \
    -keystore /usr/local/tomcat/conf/keystore.jks \
    -storepass password \
    -keypass password \
    -dname "CN=localhost, OU=Development, O=FabFlix, L=Irvine, ST=CA, C=US"

# Remove default webapps
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy the custom server.xml for HTTPS support
COPY conf/server.xml /usr/local/tomcat/conf/server.xml

# Copy the built war file
COPY --from=build /app/target/fabflix.war /usr/local/tomcat/webapps/fabflix.war

# RedissonSessionManager must be on Tomcat's classloader (not the webapp's WEB-INF/lib)
RUN mkdir -p /tmp/war-extract && \
    cd /tmp/war-extract && \
    jar xf /usr/local/tomcat/webapps/fabflix.war WEB-INF/lib && \
    cp WEB-INF/lib/*.jar /usr/local/tomcat/lib/ && \
    rm -rf /tmp/war-extract

# Expose HTTP and HTTPS ports
EXPOSE 8080
EXPOSE 8443

# Environment variables with defaults
ENV REDIS_CLUSTER_NODES=redis-node-1:6379,redis-node-2:6379,redis-node-3:6379
ENV DB_DRIVER=org.postgresql.Driver
ENV DB_URL=jdbc:postgresql://localhost:5432/fabflix
ENV DB_USERNAME=postgres
ENV DB_PASSWORD=password
ENV TMDB_API_KEY=
ENV RECAPTCHA_SECRET_KEY=
ENV RECAPTCHA_SITE_KEY=

CMD ["catalina.sh", "run"]
