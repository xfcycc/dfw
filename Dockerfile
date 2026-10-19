FROM node:24.13.1-bookworm-slim AS web-builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --no-audit --no-fund
COPY tsconfig*.json vite.config.ts ./
COPY src ./src
RUN npx vue-tsc -p tsconfig.web.json --noEmit && npm run build:web

FROM maven:3.9-eclipse-temurin-21 AS java-builder
WORKDIR /app
COPY backend ./backend
RUN mvn -f backend/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime
ENV PORT=3001 WEB_PATH=/app/dist/web
WORKDIR /app
COPY --from=java-builder /app/backend/target/party-monopoly.jar ./party-monopoly.jar
COPY --from=web-builder /app/dist/web ./dist/web
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 1000 game && useradd --uid 1000 --gid game --create-home game \
    && mkdir -p /app/data && chown game:game /app/data
USER game
EXPOSE 3001
CMD ["java", "-jar", "party-monopoly.jar"]
