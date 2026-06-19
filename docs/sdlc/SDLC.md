# SDLC traceability

| Phase | Evidence in this repository | Exit question |
|---|---|---|
| Discover | Jira story, acceptance examples, risk notes | Are we solving a valuable, understood problem? |
| Design | Architecture and API contract | Can we explain boundaries, data, failure, and security? |
| Build | Java, JavaScript, infrastructure as code | Is the change small, readable, and reversible? |
| Verify | JUnit, MockMvc, Playwright | Do tests cover behavior and meaningful failure paths? |
| Release | Jenkins artifact and container tag | Is the exact tested artifact being promoted? |
| Operate | Actuator health, AWS probe, runbook exercise | Can we detect, diagnose, recover, and learn? |
| Retire | Data/export and resource deletion plan | Can we remove it safely and prove data handling? |

Threat-model exercise: identify assets, actors, entry points, trust boundaries, and abuse cases.
Prioritize authentication, authorization, input limits, dependency scanning, secret management,
auditability, and least-privilege AWS roles before calling the capstone production-ready.
