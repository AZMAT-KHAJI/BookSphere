# Queue — Resource Booking System with Auto-Waitlist

A shared-resource booking system (think: meeting rooms, lab equipment, library
books). Users book what's free; if something's taken, they join a waitlist
and get **automatically** offered the resource — with a countdown to claim it
— the moment it's returned.

## Why this project is worth showing in interviews

Most fresher CRUD projects stop at "save data, show data." This one has two
real system-design problems baked in:

1. **Preventing double-booking under concurrency** — two users clicking
   "Book" on the same resource at the same instant must never both succeed.
   Solved with a **pessimistic DB row lock** (`SELECT ... FOR UPDATE`) inside
   a transaction (`ResourceRepository.findByIdForUpdate` +
   `BookingService.bookResource`).

2. **A time-based state machine driven by a background job** — when a
   resource frees up, the next person in the waitlist is *reserved* the slot
   and given a countdown window. If they don't confirm in time, a
   `@Scheduled` job automatically expires them and promotes the next person.
   This is the same pattern used for cart-hold timers and ticket-reservation
   windows in real production systems.

## Architecture

```
Browser (HTML/CSS/JS)
      │  REST calls
      ▼
Controllers  →  Services  →  Repositories (Spring Data JPA)  →  H2 database
                   │
                   └── WaitlistExpiryScheduler (@Scheduled, runs every 15s)
```

**Entities:** `User`, `Resource`, `Booking`, `WaitlistEntry`
**Resource states:** `AVAILABLE → BOOKED → (returned) → RESERVED (for next in line) → BOOKED`
**Waitlist states:** `WAITING → NOTIFIED → CONFIRMED` or `NOTIFIED → EXPIRED → (next person NOTIFIED)`

## How to run

You need **Java 17+** and **Maven** installed.

```bash
cd library-booking-system
mvn spring-boot:run
```

Then open **http://localhost:8080** in your browser.

The app seeds 3 demo users and 3 demo resources automatically on startup —
no manual setup needed.

(Optional) View the raw database tables live at **http://localhost:8080/h2-console**
— JDBC URL: `jdbc:h2:mem:librarydb`, username `sa`, no password. Great for
showing an interviewer the actual state changes as you click around.

## The demo script (this is what sells it in an interview)

1. **Show a normal booking:** pick a user, click "Book now" on an available
   resource. It turns red (BOOKED).
2. **Show the waitlist forming:** switch to a different user (dropdown at
   top), click "Join waitlist" on that now-booked resource.
3. **Show the auto-handoff:** switch back to the first user, click "Return"
   on their booking. Watch the resource flip to RESERVED and the second
   user's card show "Claim it — it's your turn" with a live countdown.
4. **Show the expiry-and-promote logic:** either click "Claim it" to confirm,
   *or* wait out the countdown (2 minutes by default) and refresh — the
   background scheduler will have auto-expired that person and, if a third
   person was waiting, notified them instead.
5. **Show the concurrency fix (the strongest part):** open two browser tabs
   as two different users, both looking at the same available resource.
   Click "Book now" in both tabs as close together as you can. Only one
   succeeds — the other gets a clear "not available" message instead of a
   silent double-booking bug. Point to `ResourceRepository.findByIdForUpdate`
   and `@Transactional` in `BookingService` and explain *why* that guarantees
   correctness under concurrent requests.

## Talking points for interviews

- **Why pessimistic locking here, not optimistic?** Booking conflicts are
  expected to be common on popular resources, so failing fast with a lock
  (rather than retrying after an optimistic-lock exception) gives users a
  clearer, faster experience. Trade-off: locking reduces throughput under
  very high concurrency — worth mentioning you'd revisit this with
  something like Redis-based distributed locking if scaling beyond one DB.
- **Why a scheduled job instead of checking expiry on every read?**
  Checking on read (lazy expiry) avoids a background job but means an
  expired reservation can sit stale until someone happens to look at it —
  bad if you want prompt notifications. A small periodic job (here: every
  15s) keeps state fresh without needing a full message queue for a
  project at this scale. At real scale you'd replace this with a delayed
  job/queue system (e.g., a scheduled task in SQS or a Redis TTL key).
- **How would this scale to thousands of resources / requests?** Move the
  lock to a distributed cache (Redis `SETNX`/TTL) instead of a DB row lock,
  shard the scheduled expiry check instead of scanning one table, and put
  the waitlist queue behind a proper message broker for notification
  delivery instead of polling.

