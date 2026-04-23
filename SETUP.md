# First-time setup (for a new machine)

Follow these steps **once** on a clean PC before you run the backend (`mvnw spring-boot:run`) and the frontend (`npm run dev`). Commands below show **Windows** (`cmd` / PowerShell); on Mac or Linux use `./mvnw` instead of `mvnw.cmd` and the usual `export` instead of `set`.

---

## 0. Clone the repository

```powershell
git clone <your-repo-url>
cd Mukund
```

(Use the real folder name if yours differs.)

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

## 3. Install and run MongoDB (local)

This project expects **MongoDB** reachable at **`mongodb://localhost:27017`** by default (database name **`guthi`**).

1. Install **MongoDB Community Server** for your OS ([MongoDB download center](https://www.mongodb.com/try/download/community)).
2. Start **`mongod`** (Windows: run as a service, or start `mongod.exe` with your config so it listens on **port 27017**).
3. You do **not** have to create the `guthi` database manually; the app creates data on first use.

**Check (optional):**

```powershell
mongosh
```

Then in the shell: `db.runCommand({ ping: 1 })` — you should get `{ ok: 1 }`. Exit with `exit`.

If Mongo uses another host/port, set before starting the backend:

```powershell
set MONGODB_URI=mongodb://localhost:27017/guthi
```

(PowerShell: `$env:MONGODB_URI = "mongodb://..."`.)

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
| 3 | MongoDB | `mongod` listening on 27017 (or set `MONGODB_URI`) |
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
| Mongo connection refused | Start `mongod`; confirm URI matches `guthi/src/main/resources/application-dev.yml` or `MONGODB_URI`. |

---

## Docker (optional)

You **do not** need Docker for local Mongo if you installed MongoDB on the machine. Docker is optional (e.g. `guthi/docker-compose.yml` for a containerized Mongo, or Testcontainers in tests when Docker is running).

---

## More documentation

- **`README.md`** — architecture, tech stack, routes, backup scripts overview.  
- **`oops.md`** — OOP concepts as used in the codebase.  
- **`guthi/docs/PAYMENT-QR.md`** — donation QR / PhonePe configuration.
