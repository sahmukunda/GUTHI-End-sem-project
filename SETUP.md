# First-time setup (for a new machine)

Follow these steps **once** on a clean PC before you run the backend (`mvnw spring-boot:run`) and the frontend (`npm run dev`). Commands below show **Windows** (`cmd` / PowerShell); on Mac or Linux use `./mvnw` instead of `mvnw.cmd` and the usual `export` instead of `set`.

---

## 0. Clone the repository

```powershell
git clone https://github.com/sahmukunda/GUTHI-End-sem-project.git
cd GUTHI-End-sem-project
```

---

## 1. Install Java 17 (JDK)

The backend is **Spring Boot 3.2** on **Java 17**.

1. Download a **JDK 17** build (Temurin, Oracle JDK, Microsoft Build of OpenJDK, etc.).
2. Install it and set **`JAVA_HOME`** to the JDK root (the folder that contains `bin\java.exe`).
3. Add **`%JAVA_HOME%\bin`** to your **user PATH**.

**Check:**

```powershell
java -version
```

You should see a `17.x` (or “17”) version line. If `java` is not found, fix `PATH` / `JAVA_HOME` and open a **new** terminal.

> You do **not** need a separate Maven install: the repo ships **`guthi/mvnw.cmd`** (Windows) and **`guthi/mvnw`** (Unix), which download Maven on first use.

---

## 2. Install Node.js and npm (for the frontend)

Use **Node.js 18 LTS** or newer (20 LTS is fine).

1. Install from [https://nodejs.org/](https://nodejs.org/) (include **npm** in the installer).
2. Optional but useful: enable **corepack** if you use Yarn/pnpm later (not required for this project).

**Check:**

```powershell
node -v
npm -v
```

---

## 3. Install MongoDB and start replica set `rs0` on port 27018

The backend connects to **`mongodb://localhost:27018/guthi?replicaSet=rs0`**. A normal standalone server on port 27017 will not connect, and approving a fund or land request needs a replica set because those writes are one MongoDB transaction.

1. Install **MongoDB Community Server** ([MongoDB download center](https://www.mongodb.com/try/download/community)). `mongod` and `mongosh` should be on your PATH.
2. Create a data folder and start the server. Leave this terminal open.

```powershell
mkdir C:\data\guthi-rs
mongod --port 27018 --replSet rs0 --dbpath C:\data\guthi-rs --bind_ip localhost
```

Mac or Linux:

```bash
mkdir -p ~/data/guthi-rs
mongod --port 27018 --replSet rs0 --dbpath ~/data/guthi-rs --bind_ip localhost
```

3. In a **second** terminal, initiate the set once:

```powershell
mongosh --port 27018 --eval "rs.initiate({_id:'rs0', members:[{_id:0, host:'localhost:27018'}]})"
```

**Check:** `mongosh --port 27018 --eval "rs.status().ok"` prints `1`.

You do **not** create the `guthi` database yourself. The first backend start seeds demo users and sample data. Sign in with `admin@guthi.local` / `ChangeMe123!` or `priya@guthi.local` / `ChangeMe123!` (full list in **`login.md`**).

If the replica set is not on this host and port, set **`MONGODB_URI`** before starting the backend. It must include the replica-set name, for example:

```powershell
$env:MONGODB_URI = "mongodb://localhost:27018/guthi?replicaSet=rs0"
```

---

## 4. (Optional) MongoDB Database Tools

Only needed if you want to run **`mongo-dump.ps1`** / **`mongo-restore.ps1`** from the repo. Not required just to run the app.

- Download: [MongoDB Database Tools](https://www.mongodb.com/try/download/database-tools)
- Add the `bin` folder (e.g. `C:\Program Files\MongoDB\Tools\100\bin`) to **PATH**, or set **`MONGODB_TOOLS_BIN`** to that folder for your session.

---

## 5. Install backend dependencies (first Maven resolve)

From the **`guthi`** folder, the wrapper downloads Maven and all **JAR** dependencies the first time you run a goal.

```powershell
cd guthi
mvnw.cmd -q -DskipTests dependency:go-offline
```

If that succeeds, you are ready to start Spring Boot. (Skipping tests here only speeds the first download.)

**Unix:**

```bash
cd guthi
chmod +x mvnw
./mvnw -q -DskipTests dependency:go-offline
```

---

## 6. Install frontend dependencies

```powershell
cd ..\guthi-frontend
npm install
```

This creates **`node_modules`** from `package.json` / `package-lock.json` (if present). Run this again after pulling changes that update dependencies.

---

## 7. Run the application (every day)

Use **two terminals**.

**Terminal A — backend (port 9090):**

```powershell
cd guthi
mvnw.cmd spring-boot:run
```

Wait until you see something like “Started …Application” with no stack trace.

**Terminal B — frontend (port 5173):**

```powershell
cd guthi-frontend
npm run dev
```

Open a browser: **http://localhost:5173**

The Vite dev server **proxies** `/api`, `/qra`, and `/ws` to **http://localhost:9090**, so you normally do **not** need to set `VITE_API_BASE` for local work.

---

## Quick checklist

| Step | Tool | Verify |
|------|------|--------|
| 1 | JDK 17 | `java -version` |
| 2 | Node + npm | `node -v`, `npm -v` |
| 3 | MongoDB | Replica set `rs0` on port 27018 (`rs.status().ok` is 1) |
| 4 | (Optional) DB Tools | `mongodump --version` |
| 5 | Backend deps | `mvnw.cmd dependency:go-offline` in `guthi` |
| 6 | Frontend deps | `npm install` in `guthi-frontend` |
| 7 | Run | `mvnw.cmd spring-boot:run` + `npm run dev` |

---

## Common problems

| Symptom | What to try |
|---------|-------------|
| `java` / `javac` not found | Fix `JAVA_HOME` and `PATH`; reopen terminal. |
| `mvnw` permission denied (Linux/Mac) | `chmod +x guthi/mvnw` |
| Port **9090** already in use | Stop the other process or set `SERVER_PORT` (see Spring docs). |
| Port **5173** already in use | Stop other Vite apps or change port in `guthi-frontend/vite.config.ts`. |
| Frontend loads but API errors / login fails | Ensure backend is running on 9090; check browser Network tab for `/api` calls. |
| Mongo connection refused, or `replica set` errors | Start `mongod` with `--port 27018 --replSet rs0`, then run `rs.initiate` once. The URI must be `mongodb://localhost:27018/guthi?replicaSet=rs0`. |

---

## Docker (optional)

You **do not** need Docker. `guthi/docker-compose.yml` starts a standalone MongoDB on port 27017, which does not match the default URI and cannot run approval transactions. Use the replica set in step 3. Docker is still optional for Testcontainers when you run tests.

---

## More documentation

- **`README.md`** — architecture, tech stack, routes, backup scripts overview.  
- **`oops.md`** — OOP concepts as used in the codebase.  
- **`guthi/docs/PAYMENT-QR.md`** — donation QR / PhonePe configuration.
