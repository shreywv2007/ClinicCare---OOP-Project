# ClinicCare — Member 1 database foundation

**Review required before merging.** Based on ClinicCare Shared AI Coding Context v1.0 and the visible AppointmentDAO/model code in pull request #1. Member 1's six production classes and the Maven setup are provided; no other member's source is included. The original README can link to this file. Do not overwrite existing files without checking ownership.

## Build and test before merging

Use JDK 17 or newer and Maven 3.6.3 or newer. From the repository root:

```bash
java -version
mvn -version
mvn clean test-compile
mvn exec:java -Dexec.mainClass=com.cliniccare.database.DatabaseSmokeTest -Dexec.classpathScope=test
```

The last command is a standalone JDBC test, not a JUnit test. It uses a temporary database, inserts fictional records, checks constraints and persistence, and deletes its temporary data. Expect `MEMBER 1 DATABASE SMOKE TEST PASSED` and `BUILD SUCCESS`. The test does not create the real application database. Later, Member 6's Main calls `DatabaseInitializer.initialize()` at startup. Run the application from the repo root so the database path remains consistent.

## Shared public APIs

```java
// DatabaseConnection (static, returns a new connection; caller closes it)
public static Connection getConnection() throws SQLException;
// DatabaseInitializer (static, idempotent, call at startup)
public static void initialize() throws ClinicCareException;
// Repository<T> (checked exceptions; null if ID not found)
boolean save(T entity) throws ClinicCareException;
T findById(int id) throws ClinicCareException;
List<T> findAll() throws ClinicCareException;
boolean update(T entity) throws ClinicCareException;
boolean delete(int id) throws ClinicCareException;
```

Exceptions: `ClinicCareException extends Exception`; `ValidationException` and `AppointmentConflictException` extend `ClinicCareException`. Each accepts `(String message)` and `(String message, Throwable cause)`.

DAO methods should use try-with-resources and wrap SQLException in ClinicCareException. `getConnection()` enables foreign keys for every connection. Never write SQL in Swing panels. The proposed `getConnection()` and Repository signatures match the visible AppointmentDAO in PR #1; the complete Member 4 service/panel and other branches still need review. Member 4's save uses generated keys; other DAOs should coordinate the same pattern.

## Schema (no renamed or added columns)

- patients: patient_id, name, age, gender, contact, email
- doctors: doctor_id, name, specialization, contact, consultation_fee, is_active
- appointments: appointment_id, patient_id, doctor_id, appointment_date, appointment_time, consultation_type, status
- bills: bill_id, appointment_id, amount, payment_status, created_at

All IDs are SQLite-generated integer primary keys; Java uses int, zero for new unsaved objects. Dates/times in Java: LocalDate/LocalTime; database: ISO yyyy-MM-dd and HH:mm (24-hour time, no seconds). Use `DateTimeFormatter.ofPattern("HH:mm")` to store times. Status values: CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED. Payment values: PAID, UNPAID. Active is 0/1. Store numeric fees as double in Java and REAL in SQLite. Contact is TEXT so leading zeros are not lost.

Database: `database/cliniccare.db`, automatically created; `.gitignore` excludes database files and build outputs. Override for isolated tests with `-Dcliniccare.db.path=/path/to/test.db`. Do not commit real patient data. `created_at` defaults to SQLite CURRENT_TIMESTAMP (UTC). Foreign keys prevent orphan rows, and no cascade deletes are enabled; use cancellation/deactivation rather than deleting referenced records. Schema initialization is transactional and does not perform migrations on existing database files.

## Decisions for team review before merge

- Proposed JDK 17, sqlite-jdbc 3.46.1.3, Maven compiler plugin 3.13.0, exec plugin 3.5.0. These are pinned versions, not a claim of latest. No extra application dependencies. Confirm everyone's environment. 
- Required non-null fields, age >= 0 at DB level, nonnegative numeric fees/amounts. Member 6's provisional validator uses age 0–120 and required email; coordinate upper bound and email policy. Validate date/time format, phone, email and required text before writing; the DB does not implement full format validation.
- Exact doctor/date/time conflict excluding CANCELLED (matches Member 4's visible DAO). Unique partial index enforces this even if two writes race. Coordinate friendly conflict exception handling with Member 4. No duration-based overlap checks.
- Bill rows require a valid appointment; one-bill-per-appointment policy, when bills are created, and handling cancellation are not finalized. No UNIQUE constraint on appointment_id in bills until the team agrees. No cascading deletion. Billing owner must decide a workflow that avoids accidental duplicate bills.
- Consultation type stored as non-null TEXT, not constrained to specific enum names pending agreement. PR #1 proposes GENERAL (1.0), FOLLOW_UP (0.5), EMERGENCY (1.5); Member 5 must agree before implementing charges.
- Member 4/5 must agree which DAO owns `int countUnpaidBills() throws ClinicCareException` for Member 6 dashboard; proposed count of UNPAID bills, cancellation policy pending. No billing DAO is added by Member 1.
- Member 6 ownership of ClinicManager, panel constructor/refresh callbacks and shared service methods remain subject to team agreement.
- Member 4's PR currently targets main. Retarget to develop after the foundation is merged and coordinate updates with its owner. Do not merge unreviewed code to main.

## Verification performed here

Java source and smoke test compile with `javac --release 17`. The SQL statements were also run against Python's actual SQLite engine: table creation, unpaid default, foreign key rejection, duplicate slot rejection, negative amount rejection, invalid status rejection, reuse after cancellation, persistence and reinitialization passed. Maven and the SQLite JDBC driver were not available in the preparation environment, so the Java JDBC smoke test must still be run on your machine or in Codespaces. No changes were made to GitHub.
