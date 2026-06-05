# CLAUDE.md

## Project

**Sistema di Gestione di Prenotazioni per Stabilimenti Balneari** — a reservation-management system for beach establishments (umbrella/sunbed spots). This is a university software-engineering project that follows an explicit analysis → design → implementation pipeline (textual spec → use cases → domain model → GRASP refinement). **This repo is the implementation phase.** Implement only what the analysis artifacts describe — do not invent requirements; surface gaps instead.

> **The textual project specification has been added as a Markdown file at `docs/diagrams/specifica progetto.md`.** It is the original requirements statement and the top of the analysis pipeline — read it first, alongside the diagrams, when scoping any use case.

> **Stack (confirmed against the professor's reference project):** **Java 21 · Maven · Hibernate ORM 6.6.50.Final (Jakarta Persistence) · MySQL · Swing GUI.**
> - **Packages:** **flat, no root package** (mirrors the reference): `boundary`, `controller`, `entity`, `database`, `setup` directly under `src/main/java/`. The Maven `groupId` is `it.unina`, but it is **not** used as a Java package prefix. Entry point: **`setup.Main`**.
> - **Pinned dependencies (mirror the reference):** `org.hibernate.orm:hibernate-core:6.6.50.Final`, `com.mysql:mysql-connector-j:9.4.0`, `com.intellij:forms_rt:7.0.3` (IntelliJ GUI Designer runtime for Swing).
> - Build: `mvn compile` · Test: `mvn test` · Run: launch the entry class from the IDE (the reference has **no** `exec-maven-plugin`; don't assume `mvn exec:java` is wired up).
> - Persistence: Hibernate via `src/main/resources/META-INF/persistence.xml` → **MySQL**. Mirror the reference's settings: driver `com.mysql.cj.jdbc.Driver`, URL `jdbc:mysql://127.0.0.1:3306/<schema>`, a named persistence unit (reference uses `boatyardPU`), `hibernate.hbm2ddl.auto=create`, `hibernate.show_sql`/`format_sql=true`. Keep the DB password as a local placeholder, out of VCS.
> - UI: **Swing GUI** — Boundary layer = `JFrame`/forms (IntelliJ GUI Designer `.form` files), like the reference's `MainFrame`/`Form…` classes. **Not** console/CLI.
>
> **State of alignment:** `pom.xml` currently targets **Java 24 and declares no dependencies** — it must be lowered to 21 and have the pinned dependencies added (tracked as a follow-up, see end of file). The reference project under `docs/reference-project/` has already been read; the facts above are confirmed, not assumed.

## Architecture — BCED (strict)

Four layers with one-way dependencies only:

- **Boundary (B)** — `…boundary`: user interaction via **Swing** (`JFrame`/forms, IntelliJ GUI Designer `.form` files). One boundary per actor/use-case interaction. **No** business logic, **no** persistence. Calls Control only.
- **Control (C)** — `…controller`: use-case controllers (GRASP **Controller**). Orchestrate one scenario, coordinate Entities, and drive persistence through `Registro…`/`GestorePersistenza`. **Façade** lives here (mirrors the reference's `ControllerRimessaggio`). Calls Entity + Database.
- **Entity (E)** — `…entity`: the domain model from the domain/GRASP class diagrams (GRASP **Information Expert / Creator**). Holds business rules. **Composite** and **State** live here. Pure domain — depends on nothing outward.
- **Database (D)** — `…database`: a single **generic `GestorePersistenza`** (CRUD + finders over any entity) + **`JpaUtil`** (Singleton). Wraps `EntityManager` and owns transactions internally. Depends on Entity only; returns Entities, never leaks `EntityManager` upward.

> **`Registro…` domain-service classes** (analogues of the reference's `RegistroRimessaggio`/`RegistroPosti`) wrap `GestorePersistenza` with domain logic and are what Controllers call to persist/look up aggregates — there is **no per-aggregate DAO**. Following the reference (and the «Information Expert» stereotype on the GRASP diagrams), a `Registro` lives in the **`entity`** package; this means it has an `entity → database` dependency — an accepted bend of strict BCED that the reference itself uses.

**Dependency rule:** `B → C → {E, D}` and `D → E`. Never upward. Entities never import B/C/D. Cross-cutting notification crosses outward only through an **interface owned by Entity** (dependency inversion) — see Observer/Adapter below.

### Package layout

**Create packages only when a use case needs them** — don't pre-create empty placeholders. Current layout (flat, no root package):
```
src/main/java/
├── boundary/                 # B: Swing JFrame/forms, one per use-case interaction
├── controller/               # C: use-case controllers = Façades (GRASP Controller)
├── entity/                   # E: domain model + Registro… domain services
├── database/                 # D: GestorePersistenza (generic) + JpaUtil (Singleton)
└── setup/                    # DB init/seeding + Main (entry point: setup.Main)
src/main/resources/META-INF/persistence.xml
```
Sub-packages and new layers are added per use case when needed, e.g. `entity/stabilimento` (Composite), `entity/stato` (State), `entity/notifica` (Observer + `ServizioNotifica` interface), and an `infrastructure/notifica` for the Adapter — **none of these exist yet**; create them when their use case is implemented.

`Registro…` domain-service classes live in **`entity/`** (reference idiom + «Information Expert») — see the BCED note above.

## Required design patterns — where each one goes

Use these patterns **only** at the locations below. Don't scatter extra patterns and don't substitute one for another. Before implementing, confirm placement against the flow of events for the relevant use case; **if a flow contradicts this table, follow the flow** and leave a `// NOTE:` explaining the deviation.

| Pattern | Type | Home (layer) | Applied to |
|---|---|---|---|
| **Singleton** | Creational | Database | `JpaUtil` — single `EntityManagerFactory` from a named persistence unit, `getEntityManager()` per call, `chiudi()` to close; mirror the reference's `JpaUtil` exactly (note the casing) |
| **Façade** | Structural | Control | Use-case controllers (`controller/`) expose coarse-grained operations to Boundary, orchestrating `Registro…` services + Entities (mirrors `ControllerRimessaggio`) |
| **Adapter** | Structural | Infrastructure | `ServizioNotifica` target interface (owned by Entity); concrete `…Adapter` wraps the external notification channel (email/SMS/console). **No precedent in the reference — design it ourselves** |
| **Composite** | Structural | Entity | Establishment layout tree: `ComponenteStabilimento` (component) ← `Settore`/`Zona` (composite) + `Postazione` (leaf). Built by *Configurazione stabilimento*, traversed by *Visualizzazione Mappa* |
| **Observer** | Behavioral | Entity ↔ Infra | Reservation lifecycle (or `Postazione` availability) is the **Subject**; `ServizioNotifica` (via its Adapter) is an **Observer**. Notifies on *Effettua Prenotazione* / *Gestione prenotazioni personali*. **No precedent in the reference — design it ourselves** |
| **State** | Behavioral | Entity | A lifecycle entity: e.g. `Postazione` (Libera → Prenotata → Occupata) or `Prenotazione` (Bozza → Confermata → Annullata). Drives map rendering + reservation transitions |

**Verify the State host** — `Postazione` availability vs `Prenotazione` lifecycle — against the flows of events, and pick the one the flow actually describes. Don't implement both unless both flows require it.

## Use cases in scope

Implement **in this order, one at a time**, stopping for review between each. Actors: *UtenteNonAutenticato*, *GestoreAutenticato*, *ClienteAutenticato*, *Servizio di Notifica*.

1. **Registrazione / Accesso al sistema** — *UtenteNonAutenticato* registers, then authenticates into a `Cliente` or `Gestore`.
2. **Configurazione stabilimento** — *GestoreAutenticato* builds the establishment layout → exercises **Composite**.
3. **Definizione Tariffe** — *GestoreAutenticato* defines `Tariffa` (per period / spot type).
4. **Visualizzazione Mappa → Effettua Prenotazione** — *ClienteAutenticato* views the layout map; *Effettua Prenotazione* **«extend»s** *Visualizzazione Mappa* at extension point **"Prenotazione postazioni"**, then triggers **Observer** notification via *Servizio di Notifica*.

**Model the «extend» correctly:** the map view offers a "prenota" action that invokes the reservation flow. Reservation is the optional/triggered extension, **not** part of base map viewing.

## Project materials (all under `docs/`, read-only)

- **`docs/diagrams/specifica progetto.md`** — the textual project specification (requirements statement) in Markdown → **read first**; it is the top of the analysis pipeline and the prose source the use cases and domain model derive from.
- **`docs/diagrams/`** — use case, domain class, and GRASP class diagrams → **source of truth** for classes, attributes, relationships, multiplicities, and stereotypes («Creator», «Information Expert», «Controller»). Match names exactly.
- **`docs/diagrams/sequence/`** — the 2 sequence diagrams → **reference only** for call ordering and method names in those specific flows. Not exhaustive.
- **`docs/flussi/`** — flow of events per use case → **reference only**; they drive scenario steps, alternative/exception flows, and validation rules.
- **`docs/reference-project/`** — the professor's template project: a **Swing-based "Rimessaggio" (boat-storage)** app (different domain, same conventions) → **read-only reference** for: `JpaUtil`/Singleton style, the generic `GestorePersistenza` + `Registro…` model, `persistence.xml`, Swing/forms layout, BCED idioms. **Mirror its conventions, not its domain content. Do NOT modify it, build it, or copy it wholesale.** It is not part of this project's Maven build.

## Conventions

- **Naming — Italian throughout.** ALL identifiers must be in Italian: classes, interfaces, methods, variables, fields, parameters, enums and enum constants, and package names. This includes technical/infrastructure code (persistence, adapters, controllers, forms), not just the domain. Examples: `salvaPrenotazione()` not `savePrenotazione()`/`saveReservation()`; `elencoPostazioni` not `postazioneList`/`spotList`; `GestorePersistenza`, `cliente`, `dataInizio`, `numeroPostazione`. Match the exact names and casing from the domain/GRASP diagrams. Comments, log messages, and on-screen GUI text are also in Italian. (The reference confirms Italian identifiers throughout, e.g. `salva`, `cercaPerCampo`, `aggiorna`, `RegistroRimessaggio`.)
  - Only unavoidable framework/library elements stay as-is: Java keywords, JDK/Hibernate/JPA APIs and their annotations (`@Entity`, `EntityManager`, `getInstance`, `main`), Swing types (`JFrame`, `JButton`…), and Maven/config conventions. Don't translate those.
  - **JavaBean accessor prefixes `get` / `set` / `is` stay in English** (e.g. `getCliente()`, `setDataInizio()`, `isAttiva()`) — the noun stays Italian, only the prefix is English. This keeps JPA/Hibernate property-access mapping working.
- One public class per file; the package equals its BCED layer.
- Business rules and validation live in **Entity** (Information Expert) — never in Boundary.
- **Persistence:** the generic `GestorePersistenza` owns all `EntityManager` access and runs transactions (begin/commit/rollback) **inside** its own methods; it returns Entities and never exposes `EntityManager` upward. `Registro…` services add domain logic on top; Controllers call `Registro`/`GestorePersistenza` and never touch `EntityManager` directly.
- **Entity mapping (per reference):** Jakarta annotations with **field access**; `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`; `@Enumerated(EnumType.STRING)` for enums; relationships via `@OneToMany(mappedBy = …)` / `@ManyToOne @JoinColumn` / `@OneToOne @JoinColumn`. Provide getters/setters.
- **No logging framework** — rely on `hibernate.show_sql`/`format_sql` for SQL visibility. Utility/seed classes use a private constructor to prevent instantiation.
- The notification interface is **owned by Entity**; the Adapter (infrastructure) implements it. This keeps Entity dependency-free.

## Workflow (per use case)

0. Conventions are already confirmed against `docs/reference-project/` (flat packages, `JpaUtil`, generic `GestorePersistenza` + `Registro` in `entity`, Swing) — re-skim the reference when implementing a layer to copy its idioms.
1. Read the use case's flow of events (+ its sequence diagram if one exists).
2. Identify the B/C/E/D classes it touches by cross-checking the class diagrams.
3. Implement **bottom-up**: Entity → Database (`GestorePersistenza`/`Registro`) → Control (`Controller` Façade) → Boundary (Swing form).
4. Wire the required pattern(s) for that use case per the table above.
5. Keep the change **scoped to that use case**; reuse existing Entities/`Registro` services — don't duplicate.
6. Build and run/test, then **stop for review** before starting the next use case.

## Guardrails

- Respect the BCED dependency direction — **no upward imports, ever.**
- Don't add patterns beyond the six listed, and don't skip a required one.
- Don't invent requirements absent from the spec/flows — flag the gap and ask.
- Treat everything under `docs/` (diagrams, flussi, reference-project) as **read-only**.
- For new **domain** classes not on the domain/GRASP diagrams: **ask first.** New *infrastructure* plumbing (`Registro` services, adapters, Swing forms) is fine.
- **Every identifier in Italian** (classes, methods, variables, fields, params, enums, packages) and Italian GUI text/comments — except unavoidable framework APIs. See Conventions.
- The "Visual Paradigm Enterprise (evaluation copy)" watermark in the diagram images is noise — ignore it.
