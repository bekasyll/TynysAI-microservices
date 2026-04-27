# TynysAI Backend

Microservices backend for **TynysAI** - a medical-diagnostic platform that ingests chest X-ray images, runs AI-assisted analysis, lets doctors validate the AI's findings, and produces patient-facing diagnostic reports + appointments + lab results.

## Architecture

```
┌─────────────┐
│   Browser   │
└──────┬──────┘
       │ HTTPS
┌──────▼──────────────┐    ┌──────────────────┐
│ gateway-server :8072│◄──►│ Keycloak  :7080  │  OIDC / JWT
└──────┬──────────────┘    └──────────────────┘
       │ load-balanced via Eureka
       │
┌──────┼─────────────────────────────────────────────────────┐
│      ▼                                                     │
│  user-service     xray-service ──► ai-service (FastAPI)    │
│  appointment-service             medical-record-service    │
│  notification-service                                      │
└────────────────────────────────────────────────────────────┘
       │           │           │
   Postgres    Postgres    Postgres ...   (one DB per service)
       │
   Kafka    (xray ⇄ medical-record ⇄ notification events)
```

| Component | Port | Role |
|---|---|---|
| `gateway-server` | 8072 | Spring Cloud Gateway, routes `/api/**` to services, Resilience4j circuit breakers, JWT validation, aggregates Swagger UI |
| `eureka-server` | 8070 | Service discovery |
| `config-server` | 8071 | Centralised Spring Cloud Config (native, classpath-baked) |
| `user-service` | (LB only) | Users, doctor / patient profiles, Keycloak admin operations, avatar storage |
| `xray-service` | (LB only) | X-ray uploads, AI analysis orchestration, doctor validation |
| `ai-service` | 8000 | Python / FastAPI / TensorFlow inference (`pneumonia_model.h5`) |
| `appointment-service` | (LB only) | Patient ↔ doctor appointments |
| `medical-record-service` | (LB only) | Diagnostic reports, lab results |
| `notification-service` | (LB only) | Notifications fanned out from Kafka |
| `keycloak` | 7080 | OIDC provider, `tynysai` realm |
| `kafka` | 9092 | Event bus |
| `prometheus` / `grafana` / `tempo` / `loki` / `alloy` | various | Observability stack |
| `minio` | - | (reserved for future blob storage) |

## Tech stack

- Java 21, Spring Boot 4.0.5, Spring Cloud 2025.1.1
- Spring Cloud Gateway (reactive), Resilience4j, OpenFeign
- PostgreSQL per service (5 instances), Hibernate, schema bootstrapping via `schema.sql`
- Keycloak 26.5.1 (OIDC, ROPG for the SPA, admin REST for backend operations)
- Kafka + Spring Cloud Stream (Kafka binder)
- Jib for container builds (`./mvnw compile jib:dockerBuild`)
- Micrometer + Prometheus + OpenTelemetry → Tempo + Loki + Grafana

## Repository layout

```
.
├── pom.xml                       # Maven aggregator
├── tynysai-common/               # Shared DTOs + JWT converter (ApiResponse, PageResponse, CurrentUserId, KeycloakJwtAuthenticationConverter)
├── eureka-server/
├── config-server/                # Holds per-service YAML in src/main/resources/config/
├── gateway-server/
├── user-service/
├── xray-service/
│   └── ai_service/               # Standalone FastAPI service (own Dockerfile)
├── appointment-service/
├── medical-record-service/
├── notification-service/
└── docker-compose/
    └── default/
        ├── docker-compose.yml
        ├── common-config.yml     # Shared env block (timezone, JWT, Eureka, Kafka)
        └── .env.example          # Template - copy to .env
```

## Prerequisites

- Docker / Docker Desktop
- ~8 GB RAM free for the full stack
- Java 21 + Maven only if you plan to **build from source** (otherwise the prebuilt images are pulled automatically)

## Prebuilt images

All Spring services are published to Docker Hub under [`bekasyl04`](https://hub.docker.com/u/bekasyl04):

- `bekasyl04/eureka-server-tynysai:1.0.0`
- `bekasyl04/config-server-tynysai:1.0.0`
- `bekasyl04/gateway-server-tynysai:1.0.0`
- `bekasyl04/user-service-tynysai:1.0.0`
- `bekasyl04/xray-service-tynysai:1.0.0`
- `bekasyl04/appointment-service-tynysai:1.0.0`
- `bekasyl04/medical-record-service-tynysai:1.0.0`
- `bekasyl04/notification-service-tynysai:1.0.0`

`docker-compose.yml` already references these tags - `docker compose up` will pull them on first run. The `ai-service` image is built locally from `xray-service/ai_service/Dockerfile` (TensorFlow weights are bundled in the build context).

## First-time setup

1. **Configure secrets**
   ```bash
   cp docker-compose/default/.env.example docker-compose/default/.env
   # Edit .env and replace every `replace-me` with a real value.
   ```

2. **Bootstrap Keycloak**
   - Start only Keycloak first: `docker compose -f docker-compose/default/docker-compose.yml up -d keycloak-db keycloak`
   - Open `http://localhost:7080`, sign in as `admin`/`admin`.
   - Create realm `tynysai`.
   - Create two clients:
     - `tynysai-frontend` - public, Standard + Direct Access grants enabled, Web Origins `http://localhost:3000`.
     - `tynysai-backend-admin` - confidential, service accounts on, with realm roles `manage-users`, `view-users`, `view-realm`, `query-clients`, `query-users`, `query-groups` granted to its service account.
   - Copy the backend client's secret into `KEYCLOAK_BACKEND_ADMIN_SECRET` in `.env`.
   - Create realm roles `ADMIN`, `DOCTOR`, `PATIENT` (`PATIENT` set as default).

3. **Start everything**
   ```bash
   docker compose -f docker-compose/default/docker-compose.yml up -d
   ```
   First boot pulls ~3 GB of images and brings all Spring services up. Watch with `docker ps`.

## Building from source

Only needed if you want to modify Java code and roll your own images.

```bash
./mvnw -DskipTests install
for svc in eureka-server config-server gateway-server user-service \
           xray-service appointment-service medical-record-service \
           notification-service; do
  (cd $svc && ./mvnw -DskipTests compile jib:dockerBuild)
done
```

`tynysai-common` is installed into the local Maven repo by the first command and consumed by the rest. Jib writes images directly to the local Docker daemon under the same `bekasyl04/...` tags, overriding the pulled ones until next `docker compose pull`.

To publish your own fork: edit the `<image>` template in each `pom.xml` (`bekasyl04/${project.artifactId}-tynysai:1.0.0`) and run `jib:build` instead of `jib:dockerBuild`.

## Day-to-day

| Task | Command |
|---|---|
| Rebuild a single service after code change | `(cd <svc> && ./mvnw -DskipTests compile jib:dockerBuild) && docker compose -f docker-compose/default/docker-compose.yml up -d --no-deps --force-recreate <svc>` |
| Tail logs | `docker logs -f <container-name>` (e.g. `gateway-server-ms`) |
| Probe gateway | `curl http://localhost:8072/actuator/health` |
| List gateway routes | `curl http://localhost:8072/actuator/gateway/routes \| jq` |
| Swagger UI (aggregated) | `http://localhost:8072/swagger-ui.html` |
| Grafana | `http://localhost:3030` |
| Reset all data | `docker compose down && rm -rf docker-compose/default/.data` |

## Persistent runtime data

Everything mutable is mounted under `docker-compose/default/.data/` (gitignored):

- `users-db/`, `appointments-db/`, etc. - Postgres data dirs
- `keycloak-db/` - Keycloak's Postgres
- `user-uploads/` - `{userId}/avatar/avatar.{ext}` (avatars)
- `xray-uploads/` - `{ownerId}/{analysisId}.{ext}` (X-ray images)
- `minio/` - reserved

Wipe-and-recreate is safe: `rm -rf docker-compose/default/.data` then `docker compose up -d`.

## Authentication flow

- The SPA logs in via Keycloak ROPG (`tynysai-frontend` client) - implemented in the frontend's `src/lib/keycloak.ts`.
- Each gateway request must carry the JWT in `Authorization: Bearer …`.
- Gateway and every Spring service validate the token (signature against Keycloak JWKS, issuer match, role mapping `ROLE_<UPPERCASE>` for `realm_access.roles`).
- `user-service` runs a confidential client (`tynysai-backend-admin`) for admin actions: provisioning users, role assignment, password reset, sending verification emails, ending sessions.

## Notes

- All Spring services run with `TZ=Asia/Almaty` (set in `common-config.yml`) so `LocalDateTime`-based timestamps line up with the user-facing timezone.
- `app.storage.root` resolves to `/uploads` (user-service) and `/storage` (xray-service) inside containers, both bind-mounted to `.data/`.
- Schema bootstrapping is via `schema.sql` per service (`spring.sql.init.mode: always`) - there is no Flyway/Liquibase yet.
