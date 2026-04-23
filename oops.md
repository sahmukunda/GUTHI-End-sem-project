# OOP concepts in this repository

This document maps the **five pillars** of object-oriented programming to **concrete places** in the **Guthi** codebase. The backend (**`guthi/`**, Java + Spring) is where OOP is most explicit; the frontend uses **TypeScript interfaces** and **React composition**, which align with *abstraction* and *composition* in a broader sense.

The five concepts used here:

1. **Encapsulation** — hide internal state; expose controlled behavior.  
2. **Abstraction** — separate *what* from *how* (interfaces, APIs, layers).  
3. **Inheritance** — *is-a* specialization by extending a base type.  
4. **Polymorphism** — one interface or superclass, many implementations; runtime behavior varies.  
5. **Composition** — *has-a*: build objects from other objects (prefer over deep inheritance trees).

---

## 1. Encapsulation

**Idea:** Fields are not manipulated arbitrarily by unrelated code; classes (or records/DTOs) bundle data with the operations or contracts that protect consistency.

**Where in this repo**

| Location | How |
|----------|-----|
| `com.guthi.model.*` | Mongo `@Document` types (`User`, `Donation`, `Festival`, …) use **Lombok** `@Getter` / `@Setter` (and builders where used) so persistence fields are accessed through accessors instead of scattered raw field access across the codebase. |
| `com.guthi.dto.request.*` / `dto.response.*` | Request/response DTOs expose only the fields needed for HTTP JSON; internal entity graphs are not leaked directly to clients. |
| `com.guthi.security.JwtService` / `AuthSessionService` | Token creation, parsing, and session persistence are centralized rather than duplicating crypto or header logic in controllers. |
| Services (`com.guthi.service.*`) | Business rules (e.g. balance checks, status transitions) live in services; controllers mostly **delegate** and do not reach into repositories for multi-step rules ad hoc. |

**Example to read:** `com.guthi.model.User` (credentials and profile fields with controlled exposure via Spring Security’s `UserDetails` contract).

---

## 2. Abstraction

**Idea:** Depend on **stable contracts** (interfaces, abstract Spring extension points) and hide implementation details behind them.

**Where in this repo**

| Location | How |
|----------|-----|
| `com.guthi.repository.*Repository` | Each repository is a **Spring Data** `interface` extending `MongoRepository<Entity, String>`. Callers depend on the repository *contract*; MongoDB query implementation is generated or custom-derived. |
| `com.guthi.security.AppUserDetailsService` | Implements Spring Security’s **`UserDetailsService`** — abstraction for “load user for login”. |
| `com.guthi.integration.phonepe.PhonePeDqrClient` | Hides HTTP details of PhonePe behind a small, testable client used by donation/QR flows. |
| REST controllers | Expose **resources** (`/api/...`) without exposing internal persistence shape; mapping to DTOs abstracts the domain document layout. |

**Example to read:** `DonationRepository` — interface only; compare with `DonationService` orchestration logic.

---

## 3. Inheritance

**Idea:** A derived type **extends** a base and reuses or specializes behavior (*is-a*).

**Where in this repo**

| Location | How |
|----------|-----|
| `com.guthi.model.User implements UserDetails` | Domain user **is-a** Spring Security `UserDetails` for authentication/authorization integration; required methods are implemented (`getPassword`, `getAuthorities`, …). |
| `com.guthi.security.JwtAuthenticationFilter extends OncePerRequestFilter` | Custom filter **is-a** Spring Security once-per-request filter hook. |
| `com.guthi.exception.* extends RuntimeException` | Domain-specific errors (`BadRequestException`, `ResourceNotFoundException`, …) **are-a** runtime exception for uniform handling. |
| `*Repository extends MongoRepository<…>` | Repository interfaces **inherit** CRUD and query support from Spring Data’s base interface. |
| `com.guthi.config.MongoBigDecimalConfig` | Nested converter classes implement Spring’s **`Converter<A,B>`** interface (interface inheritance) for Mongo `Decimal128` mapping. |
| `com.guthi.config.MongoAmountMigration implements CommandLineRunner` | Startup hook **is-a** Spring Boot command-line runner. |

**Example to read:** `JwtAuthenticationFilter` — `extends OncePerRequestFilter` + `@Override` of `doFilterInternal`.

---

## 4. Polymorphism

**Idea:** Code can treat different implementations **through the same supertype or interface**; the actual behavior depends on the runtime object (including Spring-injected implementations).

**Where in this repo**

| Location | How |
|----------|-----|
| Spring **`@Service` / `@Component` injection** | Controllers depend on service **interfaces** are not used much, but they depend on **concrete** services injected by type; repositories are **interfaces** whose *proxy* implementation is chosen at runtime by Spring Data. |
| `UserDetailsService` → `AppUserDetailsService` | `AuthenticationManager` works with the **`UserDetailsService`** abstraction; at runtime the app uses your polymorphic implementation. |
| `GlobalExceptionHandler` | `@ExceptionHandler` methods dispatch on **exception subtype** (`ResourceNotFoundException` vs `BadRequestException`, …) — polymorphic handling of `Throwable` hierarchy. |
| `Converter<BigDecimal, Decimal128>` (and reverse) | Mongo mapping layer selects the correct converter implementation for the type pair. |
| `HandshakeInterceptor` | `JwtWebSocketHandshakeInterceptor` supplies behavior through the WebSocket **interceptor** interface. |

**Example to read:** `GlobalExceptionHandler` — multiple `@ExceptionHandler` methods on distinct exception types, one advice bean.

---

## 5. Composition

**Idea:** Classes **contain** collaborators (fields injected or constructed) and **delegate** work to them — *has-a* / *uses-a* — instead of building large single inheritance trees.

**Where in this repo**

| Location | How |
|----------|-----|
| `com.guthi.service.*` with **`@RequiredArgsConstructor`** (Lombok) | Services **compose** repositories (`UserRepository`, `DonationRepository`, …), other services, and `LiveUpdatePublisher` as constructor dependencies — classic Spring **composition root**. |
| `DonationService` | Composes financial repositories, claim/allocation logic, and optional integration (`PhonePeDqrClient`, `QrCodeService`) rather than subclassing them. |
| `VolunteerService` | Composes `VolunteerEventRepository`, `VolunteerSignupRepository`, `UserRepository`, `LiveUpdatePublisher`. |
| Controllers | Compose services only; they do not subclass services. |
| Frontend **`AuthProvider` → `StompProvider` → `App`** | React tree **composes** providers and routes; behavior is wired by nesting, not class inheritance. |

**Example to read:** `VolunteerService` — list of `private final` dependencies injected via Lombok constructor; all behavior built from collaborators.

---

## TypeScript / React (short note)

Strict OOP *classes* are uncommon in the SPA; instead you see:

- **Abstraction:** `type` / `interface` for API payloads and props.  
- **Composition:** page components compose smaller components and hooks (`useAuth`, `useStompSubscription`).  

That mirrors the same design goals as in the Java layer: small units, clear boundaries, testable pieces.

---

## Quick cross-reference table

| Concept | Primary Java examples |
|---------|------------------------|
| Encapsulation | `model.*`, DTOs, `JwtService`, services as façade |
| Abstraction | `MongoRepository` interfaces, `UserDetailsService`, REST + DTOs |
| Inheritance | `User implements UserDetails`, `JwtAuthenticationFilter extends OncePerRequestFilter`, `* extends RuntimeException`, `*Repository extends MongoRepository` |
| Polymorphism | Spring proxies, exception handlers, `Converter` implementations |
| Composition | `@RequiredArgsConstructor` services, controller → service → repository layering |

For a deeper domain walkthrough of the Spring app, see `.cursor/skills/guthi-governance-springboot/SKILL.md` if present in your workspace.
