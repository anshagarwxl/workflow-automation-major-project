# Event Team Registration & Random Team Allocation

Workflow automation major project built on **Camunda 8**.

Students register for an event either as a self-formed team or as individuals. Self-formed teams go to their
Faculty Advisor (FA) for approval; approved teams are saved to the Approved Teams database. Students left without
a team are randomly grouped into teams once registration closes, and every random team is also sent to the FA
for confirmation.

## Team

| Member | Responsibility |
| --- | --- |
| Amol Saxena | Milestone 1: BPMN design and architecture |
| Ansh Agarwal | Milestone 2: full-stack integration and deployment |
| Samarth Ryan Edward | |

## Milestone status

| Milestone | Deadline | Status |
| --- | --- | --- |
| 1. BPMN design and architecture | 08 Nov 2026 | Submitted |
| 2. Full-stack integration and deployment | 13 Nov 2026 | In progress |
| Final technical documentation | 18 Nov 2026 | Pending |

## Milestone 1 contents

| File | What it is |
| --- | --- |
| `bpmn-models/event-team-registration.bpmn` | Two executable Camunda 8 processes in one collaboration: **Event Team Registration** (one instance per registration request) and **Random Team Allocation** (one instance per event, started by the `registration-closed` message). |
| `bpmn-models/forms/fa-team-review.form` | Camunda form the Faculty Advisor uses to approve or reject a team. Linked from the `Review team details` user task. |

Open the `.bpmn` file in Camunda Modeler (or Camunda Web Modeler) to view the diagram, then deploy it together with
the form.
