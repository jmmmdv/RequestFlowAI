# UiPath integration lab

Goal: build an attended UiPath workflow that reads goals from a CSV file, calls the planning-agent
API, and writes an audit CSV. Prefer API activities over brittle screen scraping.

## Workflow

1. Create a **Windows** process in UiPath Studio and install `UiPath.WebAPI.Activities`.
2. Add arguments `in_BaseUrl` (default `http://localhost:8080`) and `in_InputCsv`.
3. **Read CSV** into `goalsTable`, then **For Each Row in Data Table**.
4. Build JSON with `System.Text.Json.JsonSerializer.Serialize` from an object containing
   `goal` and `createWorkItems = True`.
5. Use **HTTP Request**: `POST`, endpoint `in_BaseUrl + "/api/agent/plan"`, body as JSON,
   content type `application/json`, timeout 30 seconds.
6. Parse the response and append goal, classification, IDs, UTC timestamp, and outcome to an
   audit data table. Write it with **Write CSV**.
7. Catch transport errors as system exceptions and HTTP `400` as a business exception. Retry only
   transient `429`/`5xx` responses with bounded exponential delay.

Test first with `createWorkItems = False` so the workflow is a safe dry run. Add a unique correlation
ID to the audit file and, as an advanced API exercise, extend the backend to accept an idempotency key.

UiPath Orchestrator extension: move URLs to Assets, credentials to Credential Assets, publish the
package, trigger it from a queue, and use queue reference values to prevent duplicate work.

Security rule: never place credentials in XAML, screenshots, logs, CSV files, or Git.
