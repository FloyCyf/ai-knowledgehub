# A Gateway Unified Entry Acceptance Guide

This guide is the executable acceptance checklist for member A. It verifies that all external requests go through `http://localhost:8080`, and that gateway routing, JWT authentication, admin authorization, header propagation, and article-detail rate limiting match the final implementation.

## Environment

Required local services:

| Component | Port |
| --- | --- |
| gateway-service | 8080 |
| user-service | 8081 |
| article-service | 8082 |
| ranking-service | 8083 |
| ai-service | 8084 |
| MySQL | 3306 |
| Redis | 6379 |
| RabbitMQ | 5672 / 15672 |

Start middleware first:

```powershell
docker compose up -d
docker ps
```

If `docker compose` is unavailable but legacy Docker Compose is installed, use `docker-compose up -d` instead. If both commands are unavailable, install Docker Desktop and enable the Compose v2 plugin.

Then start backend services in this order:

```powershell
cd user-service
mvn spring-boot:run

cd ..\article-service
mvn spring-boot:run

cd ..\ranking-service
mvn spring-boot:run

cd ..\ai-service
mvn spring-boot:run

cd ..\gateway-service
mvn spring-boot:run
```

Or run the helper script from the project root:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\start-all-services.ps1
```

The helper starts services in the same order, writes logs to `docs/acceptance/runtime/logs/`, and records process IDs in `docs/acceptance/runtime/service-pids.json`.
It also runs `mvn -DskipTests install` first, so local modules such as `common:1.0.0` can be resolved when each service starts.

Stop the started services with:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\stop-all-services.ps1
```

All external acceptance calls must use:

```text
http://localhost:8080
```

Do not use `8081`, `8082`, `8083`, or `8084` for final screenshots except when showing internal service startup logs.

## Automated Acceptance Script

Run from the project root:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\acceptance-gateway.ps1
```

The script verifies:

- Gateway health check.
- `POST /api/user/register`.
- `POST /api/user/login`.
- `GET /api/user/profile` returns `401` without token.
- `GET /api/user/profile` succeeds with token.
- Forged `X-User-Id`, `X-User-Role`, and `X-User-Name` headers are overwritten by the gateway.
- Normal user access to `/api/admin/**` returns `403`.
- Article draft creation, publish, latest list, and detail route through gateway.
- Ranking `GET /api/ranking/top10` route through gateway.
- AI sync, SSE, and article analysis routes through gateway.

The script writes the latest runtime record to:

```text
docs/acceptance/runtime/last-gateway-acceptance.json
```

## Admin Rate Limit Acceptance

Dynamic rate-limit update requires an admin JWT. If an admin token is available, run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\acceptance-gateway.ps1 -AdminToken "<admin-token>"
```

Expected behavior:

- `PUT /api/admin/rate-limit/article-detail` succeeds with admin token.
- The script changes article-detail limit to `windowSeconds=10`, `maxRequests=5`, `enabled=true`.
- The 6th request within the same 10-second window returns `429`.
- The script restores the default config to `windowSeconds=10`, `maxRequests=20`, `enabled=true`.

If no admin account exists, prepare one before the demo. For the default dev profile, `user-service` uses in-memory H2, so the easiest reproducible approach is:

1. Register a normal user through the gateway.
2. Open the `user-service` H2 console: `http://localhost:8081/h2-console`.
3. Use JDBC URL `jdbc:h2:mem:akh_user`.
4. Run:

```sql
UPDATE sys_user SET role = 'ADMIN' WHERE username = '<registered-username>';
```

5. Login again through `POST http://localhost:8080/api/user/login` to obtain an admin token.

## Manual Evidence Checklist

Save final screenshots under:

```text
docs/screenshots/acceptance/
```

Recommended screenshot names:

```text
01-docker-services.png
02-mvn-test.png
03-gateway-health.png
04-user-register.png
05-user-login.png
06-profile-401.png
07-profile-success.png
08-admin-403.png
09-rate-limit-config.png
10-rate-limit-429.png
11-redis-rate-limit-key.png
12-gateway-ai-route.png
```

Redis verification commands:

```powershell
docker exec akh-redis redis-cli keys "rate_limit:*"
docker exec akh-redis redis-cli hgetall "rate_limit_config:article_detail"
```

If `REDIS_PASSWORD` is configured, add `-a <password>` to `redis-cli`.

## Acceptance Result

A can mark the gateway unified-entry acceptance as passed only when:

- All final external paths use `http://localhost:8080`.
- `/api/user/**`, `/api/articles/**`, `/api/ranking/**`, and `/api/ai/**` are reachable through the gateway.
- Unauthenticated protected access returns `401`.
- Normal user access to `/api/admin/**` returns `403`.
- Admin rate-limit update works without restarting gateway.
- Article detail requests can trigger `429`.
- Runtime evidence and screenshots are saved for the final course report.
