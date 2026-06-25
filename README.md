# Beach Establishment Reservation Management System

> *Sistema di Gestione di Prenotazioni per Stabilimenti Balneari*
> University project — **Software Engineering** exam, University of Naples Federico II.

A desktop application for managing reservations of beach spots (umbrellas and associated services).
It lets customers browse the daily availability map and book umbrellas with optional extra services,
while the establishment manager configures the layout, defines pricing, and monitors incoming
reservations.

---

## Objective

This repository is the **implementation phase** of a full software-engineering pipeline
(textual specification → use cases → domain model → GRASP refinement → code). The goal is to turn
the analysis artifacts under `docs/` into working software, applying object-oriented design
principles and a strict layered architecture together with a set of required design patterns.

The system is built around two actors:

- **Customer (`Cliente`)** — registers, logs in, views the availability map for a chosen date,
  books an umbrella plus optional extra services, and manages (views/cancels) their own
  reservations.
- **Manager (`Gestore`)** — configures the establishment layout (rows of umbrellas and their
  position), sets up additional services, defines daily tariffs (with seasonal differentiation),
  and consults the reservations received.

## What it does

- **Registration & login** — users register as customers; a default manager account is seeded at
  startup. Role-based access routes each actor to their own window.
- **Establishment configuration** — the manager defines rows of umbrellas (`FilaOmbrelloni`),
  numbers the spots progressively, assigns each row a position (front / middle / back row via
  `TipoFila`), and sets up additional services with limited availability.
- **Tariff definition** — daily prices per spot type and per additional service, optionally
  differentiated by season (high / low).
- **Availability map & booking** — the customer picks a date, sees the map of available spots, and
  books an umbrella (optionally with extra services). The system prevents double-booking of the same
  spot on the same date.
- **Reservation lifecycle** — a confirmed reservation enters the `Prenotata` state; it can be
  cancelled (becoming `Annullata`) within an allowed time window before the reserved date.
- **Notifications** — upon confirmation or cancellation, the customer is notified by email through an
  external channel adapter.

## Technologies

| Area | Technology |
|---|---|
| Language | **Java 21** |
| Build | **Maven** |
| ORM / Persistence | **Hibernate ORM 6.6.50** (Jakarta Persistence) |
| Database | **MySQL** |
| GUI | **Swing** (IntelliJ GUI Designer `.form` files) |
| Email notifications | **Eclipse Angus Mail** (SMTP, via Brevo relay) |

## Architecture

The codebase follows a strict **BCED** layered architecture with one-way dependencies
**`Boundary → Control → Entity → Database`** (no layer skipping, no upward imports):

```
src/main/java/
├── boundary/      # B: Swing windows & forms (one per use-case interaction) + notification adapter
├── controller/    # C: use-case controllers / Façades (GestoreUtenti, GestoreStabilimento, GestorePrenotazioni)
├── entity/        # E: domain model + Registro… domain services
├── database/      # D: generic GestorePersistenza (CRUD/finders) + JpaUtil (Singleton)
├── notifica/      # External notification channel (COTS, sits outside the BCED chain)
└── setup/         # Main entry point + initial data seeding
```

### Design patterns

| Pattern | Layer | Applied to |
|---|---|---|
| **Singleton** | Database | `JpaUtil` — single `EntityManagerFactory` |
| **Façade** | Control | Use-case controllers expose coarse-grained operations to the GUI |
| **Adapter** | Boundary | `AdapterServizioNotifica` adapts the external email channel to the app's `DatiNotifica` message |
| **State** | Entity | `Prenotazione` lifecycle: `StatoPrenotazione` → `Prenotata` / `Annullata` |

> All identifiers, comments and on-screen text are in **Italian** by project convention.

## Getting started

### Prerequisites

- **JDK 21**
- **Maven**
- A running **MySQL** server
- **IntelliJ IDEA** recommended — the Swing forms are instrumented by the IntelliJ GUI Designer, so
  building/running through the IDE wires up the UI components correctly.

### 1. Configure the database

Create a MySQL schema (default name `stabilimenti_balneari`), then copy the persistence template and
fill in your local credentials:

```bash
cp src/main/resources/META-INF/persistence.xml.template \
   src/main/resources/META-INF/persistence.xml
```

Edit `persistence.xml` and set your MySQL `user` / `password` (and the JDBC URL / schema name if you
changed it). Hibernate is configured with `hbm2ddl.auto=update`, so the tables are created/updated
automatically on first run.

### 2. Configure email notifications (optional)

To send real confirmation/cancellation emails via SMTP, copy the email template and add your
credentials:

```bash
cp src/main/resources/email.properties.template \
   src/main/resources/email.properties
```

Fill in your Brevo SMTP login, key, and a verified sender address.

### 3. Build and run

```bash
mvn compile
```

Run the entry point **`setup.Main`** from your IDE. On startup the application seeds a default
manager account and opens the main window.

> Note: there is no `exec-maven-plugin` configured — launch `setup.Main` from the IDE rather than via
> `mvn exec:java`. Running through IntelliJ is recommended so the GUI Designer forms are instrumented.

### Default manager account

| Field | Value |
|---|---|
| Email | `gestore@stabilimento.it` |
| Password | `gestore123` |

Customers create their own accounts through the registration form.

## Usage flow

1. **As manager** — log in with the default account, configure the umbrella rows and additional
   services, then define the tariffs (per spot type / service, by season).
2. **As customer** — register and log in, pick a date, view the availability map, select an umbrella
   and any extra services, and confirm. Confirmation triggers an email notification.
3. **Manage reservations** — customers can review their reservation history and cancel a booking
   within the allowed window; managers can consult the reservations received.

## Use cases implemented

1. Registration / system access
2. Establishment configuration
3. Tariff definition
4. Map visualization → reservation (with «extend» on the map view) + notification on confirmation

---

