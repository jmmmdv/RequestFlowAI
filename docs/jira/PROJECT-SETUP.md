# Jira project setup

Create a Scrum project with key `AMC`. Configure this hierarchy:

- Epic: Intelligent Mission Planning
- Story: As a delivery lead, I want a goal decomposed into work so that execution is visible.
- Tasks: API contract, agent classification, tool invocation, UI feedback, automated tests.
- Bug: Agent creates duplicate tasks when a request is retried.

Suggested workflow: `Backlog → Ready → In Progress → Review → Done`, with a blocked flag rather
than a separate “Blocked” status. Add components `api`, `ui`, `agent`, `quality`, `platform`, `rpa`.

Sample acceptance criteria:

```gherkin
Given a valid delivery goal
When I ask the agent to create a plan
Then the response classifies the goal
And three traceable work items are created
And each action appears in the reasoning trace
```

Practice JQL:

```text
project = AMC AND sprint in openSprints() AND statusCategory != Done ORDER BY priority DESC
project = AMC AND type = Bug AND created >= -30d ORDER BY priority DESC
project = AMC AND Flagged is not EMPTY
```

Link commits and pull requests to the issue key, but keep the requirement and acceptance evidence
in Jira rather than hiding it in commit messages.
