# Guthi Governance Platform

Monorepo for a **Nepali Guthi–style community governance** demo: pooled donations, community fund requests, volunteer events and signups, festivals and reviews, peer work posts and acceptances, and **JWT-based** access for **community members** (`USER`) and **Guthi workspace** staff (`ADMIN`). The UI is a **React** SPA; the API is **Spring Boot 3** with **MongoDB**.

---

## Repository layout

| Path | Role |
|------|------|
| `guthi/` | Spring Boot backend (`com.guthi`), Maven, MongoDB, WebSocket live updates |
| `guthi-frontend/` | Vite + React + TypeScript SPA |
| `guthi/scripts/` | MongoDB BSON dump/restore helpers (`mongo-dump.ps1`, `mongo-restore.ps1`, shell variants, `mongo-tool-path.ps1`) |
| `guthi/docs/` | Extra documentation (e.g. `PAYMENT-QR.md` for UPI / PhonePe DQR) |
| `mongo-dump.ps1` / `mongo-restore.ps1` (repo root) | Thin wrappers that call `guthi/scripts/` |
| **`SETUP.md`** | **Step-by-step install** (JDK, Node, Mongo, first `mvnw` / `npm install`) for a new machine before `spring-boot:run` and `npm run dev` |

---

## Tech stack

### Backend (`guthi`)

| Layer / concern | Technology |
|-----------------|------------|
| Runtime | **Java 17** |
| Framework | **Spring Boot 3.2.5** |
| HTTP API | **Spring Web** (`RestController` / MVC) |
| Data store | **MongoDB** via **Spring Data MongoDB** (`MongoRepository`, `MongoTemplate` / aggregations where needed) |
| Security | **Spring Security** + **JWT** (**jjwt** 0.12.x), bcrypt passwords, role `USER` / `ADMIN` |
| Validation | **Jakarta Bean Validation** (`spring-boot-starter-validation`) |
| Real-time | **Spring WebSocket** + **STOMP**; clients use `@stomp/stompjs` |
| JSON | **Jackson** (default) |
| QR (donations) | **ZXing** (`javase`) |
| Payments (optional) | **PhonePe Dynamic QR** HTTP client under `com.guthi.integration.phonepe` (see `docs/PAYMENT-QR.md`) |
| Build | **Maven** (`mvnw` / `mvnw.cmd`) |
| Boilerplate reduction | **Lombok** (`@Value`, `@Builder`, `@RequiredArgsConstructor`, entity getters/setters) |
| Tests | **JUnit 5**, **Spring Boot Test**, **MockMvc**; **Testcontainers** (`mongodb`, `junit-jupiter`) when Docker is available |

### Frontend (`guthi-frontend`)

| Concern | Technology |
|---------|------------|
| UI library | **React 18** |
| Language | **TypeScript ~5.4** |
| Build / dev server | **Vite 5** |
| Routing | **react-router-dom** v6 |
| Live updates | **@stomp/stompjs** over proxied WebSocket |
| Styling | Global **CSS** in `src/index.css` (design tokens, layout, components) |

### Data & ops

| Concern | Technology |
|---------|------------|
| Database | **MongoDB** — typically a **locally installed** `mongod` on `localhost:27017` (see `application-dev.yml`). Optional: `mongo:7` via `guthi/docker-compose.yml`, or **Atlas** / any URI via **`MONGODB_URI`**. |
| Snapshots | **mongodump** / **mongorestore** (MongoDB Database Tools) — see header comments in `guthi/scripts/mongo-dump.ps1` and `mongo-restore.ps1` |

---

## Features (high level)

- **Donations**: pool balance, personal totals, UPI-style QR (`/qra`), optional PhonePe DQR; admin claim verification.
- **Community requests**: members submit; admins approve/reject and funds move against the pool where applicable.
- **Volunteer events**: admin creates events; members sign up; live volunteer lists over WebSocket.
- **Work posts**: members post help offers; others accept.
- **Festivals**: calendar-style listings, completion, reviews.
- **Auth**: separate login flows for community (`/login/user`) and Guthi workspace (`/login/guthi`). There is no public registration. A technician inserts the account into the `users` collection before that person can sign in.

---

## Prerequisites

- **JDK 17**
- **Node.js 18+** (or current LTS) and npm
- **MongoDB** installed and running locally (or any reachable instance); default dev URI: `mongodb://localhost:27017/guthi`
- **MongoDB Database Tools** on the host (or on `PATH` / **`MONGODB_TOOLS_BIN`**) for `guthi/scripts/mongo-dump.ps1` — the scripts can fall back to Docker **only** if you use Compose for Mongo and Docker is running
- **Docker**: not required for day-to-day dev if you use local Mongo; optional for CI-style tests (**Testcontainers**) or if you prefer a containerized Mongo instead of a local install

---

## Configuration

### Backend profiles and YAML

- Default profile in `guthi/src/main/resources/application.yml` is **`dev`** (Mongo URI, seed flags, JWT secret for local use).
- **`application-dev.yml`**: `spring.data.mongodb.uri` (override with **`MONGODB_URI`**), `guthi.*` (JWT, invites, PhonePe, offline UPI, etc.).
- **`application-test.yml`**: test Mongo URI (`guthi_test` by default).

**Production**: set a strong **`guthi.jwt.secret`** (or env equivalent), real Mongo URI, and payment keys only via secure configuration — never commit secrets.

### Frontend env

- **`VITE_API_BASE`**: if set, the API client uses this base URL; if empty, Vite **proxies** `/api`, `/qra`, and `/ws` to `http://localhost:9090` (see `vite.config.ts`).

---

## Run locally

> **New machine?** Follow **`SETUP.md`** first to install JDK 17, Node.js, MongoDB, and to run initial `mvnw` / `npm install` steps.

### 1. MongoDB (local install — default for this project)

Use your **locally installed** MongoDB (e.g. Windows service or `mongod` from `D:\MongoDB\bin` or Program Files). Start the server so it listens on **`localhost:27017`** (or whatever you set in **`MONGODB_URI`**).

The **`guthi`** database does not need to exist beforehand: Spring Data will create collections on first write. You can still create an empty `guthi` database in Compass or `mongosh` if you prefer.

**Docker is not part of the default workflow** here. The repo includes `guthi/docker-compose.yml` only as an **optional** way to run Mongo in a container if you do not use a local `mongod`:

```bash
cd guthi
docker compose up -d
```

If you use local Mongo only, skip the compose step entirely.

### 2. Backend (port **9090** by default)

```bash
cd guthi
./mvnw spring-boot:run
# Windows:
mvnw.cmd spring-boot:run
```

Active profile **`dev`** loads `application-dev.yml`. Override Mongo:

```bash
set MONGODB_URI=mongodb://localhost:27017/guthi
mvnw.cmd spring-boot:run
```

### 3. Frontend (port **5173**)

```bash
cd guthi-frontend
npm install
npm run dev
```

Open `http://localhost:5173`. With default Vite proxy, the SPA talks to the backend on **9090**.

### 4. Smoke-test API

With the app up:

```bash
cd guthi
mvnw.cmd test -Dtest=ApiEndpointSmokeTest
```

(Requires a running Mongo instance compatible with the test profile.)

---

## URLs (SPA)

| Area | Path |
|------|------|
| Community home | `/u` |
| Guthi workspace (admin UI) | `/guthi` |
| Logins | `/login/user`, `/login/guthi` |

Legacy `/admin/*` redirects to `/guthi/*`.

---

## API and real-time

- **REST**: under **`/api/**`** (JSON). JWT sent as `Authorization: Bearer <token>` from the SPA (`src/api/client.ts`).
- **WebSocket**: STOMP broker exposed under **`/ws`** (proxied by Vite in dev). Topics are declared in `guthi-frontend/src/live/topics.ts` and published from `com.guthi.live.LiveUpdatePublisher` after commits where applicable.

---

## Data backup and restore

BSON dumps are written to **`guthi/mongo-snapshots/bson-dump/guthi/`** (gitignored via `guthi/.gitignore`).

From repo root (Windows), after setting **`MONGODB_TOOLS_BIN`** if tools are not on PATH:

```powershell
.\mongo-dump.ps1
.\mongo-restore.ps1
```

See the header comments in `guthi/scripts/mongo-dump.ps1` and `mongo-restore.ps1` for Docker fallback, **`GUTHI_NO_DOCKER`**, **`MONGODB_URI`**, and **`MONGODB_TOOLS_BIN`**.

---

## Backend package map (concise)

| Package | Purpose |
|---------|---------|
| `com.guthi.controller` | Public REST controllers |
| `com.guthi.controller.admin` | Admin-only REST |
| `com.guthi.service` | Business logic |
| `com.guthi.repository` | Spring Data Mongo repositories + aggregation helpers |
| `com.guthi.model` | `@Document` entities |
| `com.guthi.dto.request` / `dto.response` | API shapes, validation |
| `com.guthi.security` | JWT filter, session store, `UserDetailsService` |
| `com.guthi.config` | Security, CORS, Mongo converters, static QR assets, dev seed |
| `com.guthi.exception` | Typed exceptions + `GlobalExceptionHandler` |
| `com.guthi.live` | WebSocket/STOMP publishing |
| `com.guthi.integration.phonepe` | PhonePe DQR client |

---

## Frontend map (concise)

| Path | Purpose |
|------|---------|
| `src/pages/user/*` | Community portal pages |
| `src/pages/admin/*` | Guthi workspace pages |
| `src/auth/*` | JWT storage, `AuthProvider`, `ProtectedRoute` |
| `src/api/client.ts` | `fetch` wrapper, JSON, auth header |
| `src/live/*` | STOMP subscription context + topic merge helpers |

---

## Further reading

- **`oops.md`** — how classical **OOP** concepts show up in this codebase (Java-focused, with a short TypeScript note).
- **`guthi/docs/PAYMENT-QR.md`** — offline UPI vs PhonePe DQR and env variables.

---

## License / disclaimer

This repository is a **development / teaching** style project. Replace default JWT secrets, tighten CORS, and harden payment and PII handling before any real deployment.
