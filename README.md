# Queue — Resource Booking System with Auto-Waitlist

Queue is a shared-resource booking application for things like meeting rooms, lab equipment, or library items. Users can book available resources instantly, and if a resource is already taken, they can join a waitlist and are automatically notified — with a claim window — the moment it becomes free.

---

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Usage](#usage)
- [Database Access](#database-access)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [Roadmap](#roadmap)
- [License](#license)

---

## Features

- **Instant booking** for available resources
- **Automatic waitlisting** for resources currently in use
- **Time-boxed claim window** when a waitlisted resource becomes available, with automatic expiry and promotion to the next person in line
- **Concurrency-safe booking** — simultaneous booking attempts on the same resource are handled safely; only one request can ever succeed
- **In-memory demo data** seeded automatically on startup, so the app is usable immediately with no manual setup

---

## Architecture

```
Browser (HTML / CSS / JS)
        │
        │  REST API
        ▼
Controllers  →  Services  →  Repositories (Spring Data JPA)  →  H2 Database
                    │
                    └── Scheduled Job: expires unclaimed waitlist turns (runs every 15s)
```

**Core entities:** `User`, `Resource`, `Booking`, `WaitlistEntry`

**Resource lifecycle:**

```
AVAILABLE → BOOKED → (returned) → RESERVED (held for next in line) → BOOKED
```

**Waitlist entry lifecycle:**

```
WAITING → NOTIFIED → CONFIRMED
             │
             └──→ EXPIRED → next entry becomes NOTIFIED
```

---

## Tech Stack

| Layer         | Technology                              |
|---------------|-------------------------------------------|
| Backend       | Spring Boot, Spring Data JPA               |
| Database      | H2 (in-memory)                             |
| Concurrency   | Pessimistic row locking (`SELECT ... FOR UPDATE`) within `@Transactional` boundaries |
| Scheduling    | Spring `@Scheduled`                        |
| Frontend      | HTML, CSS, vanilla JavaScript              |
| Build Tool    | Maven                                      |

---

## Getting Started

### Prerequisites

- Java 17 or later
- Maven

### Installation & Run

```bash
git clone <repository-url>
cd library-booking-system
mvn spring-boot:run
```

The application will be available at:

```
http://localhost:8080
```

On first run, the app seeds demo users and demo resources automatically — no manual configuration is required.

---

## Usage

1. Select a user from the dropdown to act as that user.
2. Browse the list of resources and their current status.
3. Click **Book now** on an available resource to reserve it.
4. Click **Join waitlist** on a resource that's currently booked.
5. Click **Return** to release a resource you're holding.
6. When a resource you're waitlisted for becomes available, you'll see a **Claim it** prompt with a countdown. Confirm within the window, or the reservation passes automatically to the next person in the queue.

Multiple browser tabs (or sessions, using different selected users) can be used to simulate concurrent users interacting with the same resource.

---

## Database Access

The application uses an in-memory H2 database that resets on every restart. To inspect it directly:

```
http://localhost:8080/h2-console
```

| Field    | Value                     |
|----------|---------------------------|
| JDBC URL | `jdbc:h2:mem:librarydb`  |
| Username | `sa`                      |
| Password | *(none)*                  |

---

## Project Structure

```
src/main/java/com/example/library/
├── controller/    REST API endpoints
├── service/       Business logic (booking, waitlist management)
├── repository/    Spring Data JPA repositories
├── model/         JPA entities (User, Resource, Booking, WaitlistEntry)
├── dto/           Request/response data transfer objects
├── scheduler/     Scheduled job for waitlist expiry
└── config/        Application configuration and demo data seeding
```

---

## Configuration

Key behavior (e.g. the waitlist claim countdown duration) is configurable via `application.properties`. Refer to that file for current defaults and available overrides.

---

## Roadmap

- Replace H2 with a persistent database (e.g. PostgreSQL) with Flyway migrations
- Add authentication and authorization (Spring Security)
- Replace client-side polling with WebSocket-based real-time notifications
- Add automated test coverage for concurrency scenarios

