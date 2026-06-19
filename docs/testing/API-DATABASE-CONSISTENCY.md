# API–database consistency tests

`ApiDatabaseConsistencyTest` compares the public REST API with the data actually stored in H2.
The API is exercised through Spring `MockMvc`; database values are read independently with raw SQL
through `JdbcTemplate`.

## Samples

| Direction | Scenario | What is compared |
|---|---|---|
| Database → API | Read one resource | ID, title, description, priority, status, timestamp |
| Database → API | Read the collection | Row count, IDs, and every business field |
| API → Database | Create a resource | Response fields, persisted fields, `Location` header |
| API → Database | Update and delete | Updated fields and physical removal of the row |

Run only these tests:

```bash
./mvnw -Dtest=ApiDatabaseConsistencyTest test
```

The console prints a small comparison table for each successful scenario. Machine-readable JUnit
XML and text reports are generated in `target/surefire-reports/` for Jenkins and other CI tools.

These tests intentionally compare business data. For POST and PUT responses, `updatedAt` is not
compared immediately because a database may normalize timestamp precision. The database → API GET
test compares it after the value has been read back from storage.

Production extension: run the same suite against PostgreSQL with Testcontainers. Keep H2 tests for
fast feedback, but use the production database engine in CI to catch SQL, type, and precision differences.
