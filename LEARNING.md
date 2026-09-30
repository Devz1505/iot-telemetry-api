# Making this project yours

This project was scaffolded with an AI coding assistant. That is normal now, and
interviewers know it. What they test is whether **you** understand the code and
can change it. This guide gets you there. Work through it before the project
goes on your CV.

**The bar:** you can explain any file without opening it, and you have added at
least three features yourself (Part 4), each in its own git commit.

---

## Part 1: Get it running (day 1)

1. `mvn verify`: all 23 tests should pass. Read the test names in the output.
2. `mvn spring-boot:run`, then `python tools/simulate_device.py` in a second terminal.
3. Open http://localhost:8080 and watch the chart. Open `/swagger-ui.html` and call
   every endpoint by hand. Deliberately send bad data: a missing `value`, humidity 140,
   an unknown device, `sensorType=SMELL`. Read each error body.
4. Open `/h2-console` and run `SELECT * FROM readings ORDER BY recorded_at DESC`.
   Find the index in `Reading.java` and relate it to the query.

## Part 2: Trace one request end to end (day 2)

Follow `POST /api/devices/esp32-lab-01/readings` with a pen and paper:

| Step | File | What happens |
|---|---|---|
| 1 | Tomcat → `DispatcherServlet` | Spring matches the URL + method to a controller method |
| 2 | `TelemetryController.ingest` | JSON → `ReadingBatchRequest` (Jackson). `@Valid` runs Bean Validation; failure → `MethodArgumentNotValidException` → 400 |
| 3 | `TelemetryService.ingest` | `@Transactional` opens a DB transaction. Looks up the device (404 if missing), checks plausibility and clock skew |
| 4 | `ReadingRepository.saveAll` | Hibernate queues INSERTs |
| 5 | `device.markSeen(now)` | No `save()` call: *dirty checking* writes the change at commit |
| 6 | `AlertService.evaluate` | Loads this device's rules **once** into an `EnumMap`, creates `Alert`s |
| 7 | Method returns | Transaction commits: readings + alerts + lastSeenAt together, or nothing |
| 8 | Back in controller | `IngestResponse` → JSON, `201 Created` |
| ✗ | `GlobalExceptionHandler` | Any exception above becomes problem+json with the right status |

Then do the same for `GET /stats` and for `POST /api/alerts/{id}/acknowledge`.

## Part 3: Concepts you must be able to explain

Try to answer each one aloud first, then check the code.

**Spring core**
- *What is dependency injection? Why constructor injection?* Spring creates the objects
  (beans) and passes their dependencies in. With a constructor, dependencies are `final`
  and explicit, and tests can pass mocks (see `AlertServiceTest`).
- *What does `@Transactional` actually do?* Spring wraps the bean in a proxy that begins
  a transaction before the method and commits after, or rolls back on a
  RuntimeException. Trap: calling a `@Transactional` method **from the same class**
  bypasses the proxy.
- *Why `readOnly = true` on queries?* Hibernate skips dirty checking, and the database
  may optimise read-only transactions.

**JPA / SQL**
- *Surrogate vs natural key?* `id` (auto-generated) vs `deviceId` (meaningful, unique).
  Foreign keys use the small, never-changing surrogate.
- *What is `FetchType.LAZY`, and what is `LazyInitializationException`?* The related
  row is loaded only when first used, which must happen inside an open session. That
  is why `open-in-view=false` and mapping to DTOs inside services matter.
- *What is the N+1 problem? Where is it prevented here?* Listing 50 alerts plus one
  device query each makes 51 queries. `@EntityGraph(attributePaths = "device")` in
  `AlertRepository` fetches them with one join instead.
- *Why compute min/max/avg in JPQL, not Java?* So you don't transfer 100k rows to
  average them. The database is built for this.
- *Why the composite index `(device_id, sensor_type, recorded_at)`?* It matches the
  WHERE and ORDER BY of the most common query. Column order matters.
- *What does `ddl-auto` do, and why not use it in production?* It generates the schema
  from the entities. Production uses versioned migrations (Flyway), so changes are
  reviewed and repeatable.

**REST / HTTP**
- *201 vs 200, 400 vs 404 vs 409, 204?* Find each one in the code and justify it.
- *PUT vs POST?* PUT is idempotent: setting the same threshold twice has the same effect
  as once. POST creates something new each time.
- *Why DTOs instead of returning entities?* It keeps a stable contract, hides internal
  fields, and avoids lazy-loading and infinite-recursion surprises in JSON.
- *Why paginate?* A device sending every 15 s makes 5,760 readings a day. Never
  return unbounded lists.

**Embedded ↔ backend (your unique angle, lean on it)**
- *Why batch readings per request?* The Wi-Fi radio is the biggest power draw, so
  there are fewer TLS/HTTP round-trips.
- *Why is `recordedAt` optional? Why reject future timestamps?* No RTC until NTP syncs,
  and an unsynced clock says 1970 or garbage.
- *Why does a NaN from the DHT22 never reach the database?* The firmware skips it, and
  `@NotNull Double` rejects null.
- *Why a plausibility range per `SensorType`?* A floating data pin or a missing pull-up
  produces impossible values. Rejecting them keeps averages honest.

**Testing**
- *Unit vs `@DataJpaTest` vs `@SpringBootTest`?* Speed vs realism. Know what each test
  class starts and why.
- *What does Mockito's `verifyNoInteractions` prove in `AlertServiceTest`?* That
  invalid input is rejected before touching the database.
- *Why does each integration test register its own device?* Tests share one database;
  unique data keeps them independent and order-free.

## Part 4: Your turn (these make it genuinely yours)

Do **at least three**, each on its own branch, with tests, as your own commits.
They are ordered roughly easy → hard.

1. **Alert hysteresis** *(recommended first, and a real embedded concept)*. Right now a
   device sitting at 31 °C with max 30 raises an alert **every reading**. Change it so
   only one open alert exists per (device, sensor type). It clears when the value drops
   below `max - hysteresis`. Add a test that sends 10 hot readings and expects 1 alert.
2. **Device API keys**. On registration, generate a random key, return it **once**, and
   store only its SHA-256 hash. Require an `X-Device-Key` header on `POST /readings`,
   returning 401 if missing or wrong. Start with a `HandlerInterceptor`; Spring Security
   comes later. Update the ESP32 sketch to send the header.
3. **Offline buffer in the device client**. In `tools/simulate_device.py`, when the API
   is unreachable, keep up to N readings with their timestamps in a ring buffer
   (`collections.deque(maxlen=N)`). When it is back, send them as one batch. Stop the
   server for a minute to test it. Explain why `recordedAt` and `receivedAt` now differ.
   Bonus: port the same logic to the ESP32 sketch and let the Firmware CI prove it compiles.
4. **Flyway migrations**. Add `flyway-core`, write `V1__init.sql` by hand, and set
   `ddl-auto=validate`. You will learn the schema properly.
5. **Data retention job**. Add an `@Scheduled` task that deletes readings older than 30
   days, with a configurable property. Test it.
6. **Docker Compose + Testcontainers**. Run the app with a real PostgreSQL and add one
   integration test against a Postgres container.
7. **MongoDB variant** *(matches the Jtronix JD)*. Store raw readings in MongoDB with
   Spring Data MongoDB, keeping devices and rules in SQL. Be ready to argue when you
   would choose each.

## Part 5: Git and CV (only after Part 4)

```bash
git init
```

```bash
git add . && git commit -m "Initial scaffold: telemetry API, tests, dashboard, firmware"
```

Then commit each of your Part 4 features separately, push to GitHub, and check that the
CI run in the repository's **Actions** tab is green.

If an interviewer asks how you built it, be straightforward: *"I scaffolded the base
with an AI assistant, then studied every layer and built X, Y and Z myself. For
example, the hysteresis logic works like this..."* That answer is credible and
increasingly expected. Claiming you wrote every line alone is not, if you can't
back it up.
