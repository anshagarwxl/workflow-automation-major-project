# Demo guide

A 15-minute walkthrough that covers every evaluation point, in the order the internal presentation asks for.
Start the stack first (`docker compose up -d --build`) and open four browser tabs:

| Tab | URL |
| --- | --- |
| Portal | http://localhost:3000 |
| Camunda Operate | http://localhost:8080/operate (demo / demo) |
| Mailbox | http://localhost:8025 |
| Swagger | http://localhost:8081/swagger-ui.html |

In the portal, switch to **Event Coordinator → Control center** and make sure the SLA preset is **Demo**
(timers in minutes) and both failure simulations are off.

## 1. Business problem and solution (1 min)

Manual event registration: approvals by email, nobody chases pending decisions, students without a team are
grouped by hand after the deadline. The solution turns each registration into a Camunda process instance with
rules, verification, SLA timers, escalation, notifications and an audit trail.

## 2. BPMN architecture and mandatory constructs (3 min)

Open the three diagrams in Camunda Modeler (`bpmn-models/`) or in the README and point at:

- **Main process** `event-team-registration` with lanes (Student, System, Faculty Advisor, Event Coordinator).
- **Sub-process + error boundary:** *Verify registration*, `REGISTRATION_INVALID`.
- **Sub-process + timer boundaries:** verification timeout → manual verification; FA review SLA → escalation.
- **Parallel multi-instance:** REST connector per member; call activity per random team.
- **Sequential multi-instance:** one email per member in `notify-team`.
- **Call activities:** `notify-team` reused three times; `event-team-registration` reused by the allocation.
- **Connectors + secrets:** REST connector with `{{secrets.STUDENT_RECORDS_URL}}`; Email connector with `{{secrets.SMTP_*}}`.
- **FEEL:** `all(memberChecks.eligible)`, `cycle(duration(sla.faReminderAfter))`, the `string join(for … in …[filter])` script, the call activity input contexts.

## 3. Happy path, end to end (3 min)

1. Persona **Student → Aarav Sharma**. *Register* → CodeSprint Hackathon → *I have a team* → add Diya Iyer
   (`RA2411003010002`) and Kabir Nair (`RA2411003010003`) with the dynamic member builder (directory lookup fills
   name and email) → FA Dr. Priya Raman → *Submit*.
2. The tracker opens: show the live BPMN (green = done, pulsing blue = active), the stepper and the audit trail.
   Point out the three parallel REST calls in the timeline and in Operate.
3. Persona **Faculty Advisor → Dr. Priya Raman** → *Tasklist*. Open the task: verification badges per member,
   SLA countdown, the linked Camunda form id. Try *Reject* without remarks (blocked), then *Approve*.
4. Back on the tracker: *Save team* → `notify-team` child (click it under "Called process instances") → three
   emails sent one after another. Show them in Mailpit and the team under *Approved teams*.

## 4. Exception handling (4 min)

| Show | How |
| --- | --- |
| Rule violation | Register a team of 2 for CodeSprint with an FA who advises neither member → *Invalid* with both reasons, emails sent |
| REST 404 | Add a member `RA2411003099999` → error expression turns the 404 into `REGISTRATION_INVALID` |
| Ineligible member | Add `RA2411003010023` (suspended) → FEEL gateway → error end event |
| Outage → incident → timer | Control center: *Student Records outage* on; submit a team → incidents appear on the tracker and in Operate; after 1 minute the timer cancels the sub-process and creates *Verify registration manually* for the coordinator. Turn the outage off, complete the verification, the team continues to FA review |
| Reminder + escalation | Submit a team and leave it: a reminder email per minute; after 3 minutes the coordinator receives *Review escalated team* |
| Member conflict | Control center: *Member conflict on next approval*; approve any team → `MEMBER_CONFLICT` boundary → auto-rejection with the reason |

## 5. Random allocation (2 min)

1. As students, register 4–7 people individually for **Tech Quiz Bowl** (*I need a team*). They appear in the
   event's *Unallocated pool* tab.
2. As **Event Coordinator**, open the event → *Close registration*. The *Random allocation* tab shows the
   allocation instance live: random teams created, one child registration per team (parallel multi-instance call activity).
3. As the FAs, approve some random teams and reject one. Rejected members return to the pool and are regrouped in
   round 2 automatically; when nobody is left the coordinator gets the summary email.

## 6. Backend, integration and Docker (2 min)

- `worker/RegistrationWorkers.java`: typed `@JobWorker`s, `@VariablesAsType` records, `ZeebeBpmnError`.
- `workflow/WorkflowEngine.java`: the single integration point with Camunda (gRPC commands + Orchestration Cluster REST searches).
- `service/TaskInboxService.java`: the custom Tasklist on top of Camunda user tasks; task listeners keep statuses in sync.
- `docker-compose.yml`: 8 services, health-checked start order, secrets from `.env`, connector secret prefix.
- `docker compose ps` to show everything healthy.

## Questions to be ready for

- *Why both a database and process variables?* The engine owns the flow; PostgreSQL is the read model for
  queries (approved teams, pool, history) that should not depend on the engine's secondary storage.
- *What happens if a worker crashes mid-job?* The job times out and is re-activated; workers are idempotent
  (for example, saving an approved team twice returns the existing team).
- *How would this scale?* More Zeebe partitions and brokers, several backend replicas sharing the job load,
  managed PostgreSQL, Camunda SaaS or Helm on Kubernetes; the BPMN does not change.
- *Why a sequential multi-instance for emails?* SMTP providers rate-limit; one-by-one delivery keeps the order and
  makes a single failure easy to locate.
