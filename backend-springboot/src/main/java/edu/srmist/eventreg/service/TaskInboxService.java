package edu.srmist.eventreg.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.srmist.eventreg.domain.Registration;
import edu.srmist.eventreg.repository.RegistrationRepository;
import edu.srmist.eventreg.web.ApiException;
import edu.srmist.eventreg.web.dto.Requests.CompleteTaskRequest;
import edu.srmist.eventreg.web.dto.Views.*;
import edu.srmist.eventreg.workflow.ProcessConstants;
import edu.srmist.eventreg.workflow.WorkflowEngine;
import io.camunda.client.api.search.enums.UserTaskState;
import io.camunda.client.api.search.response.UserTask;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Backs the custom Tasklist inbox. Reads Camunda user tasks through the Orchestration Cluster API, enriches them
 * with the registration from the database, and completes them with validated, typed variables.
 */
@Service
public class TaskInboxService {

  private final WorkflowEngine engine;
  private final RegistrationRepository registrations;
  private final ViewMapper views;
  private final AuditService audit;
  private final ObjectMapper json;
  private final TransactionTemplate tx;

  public TaskInboxService(WorkflowEngine engine, RegistrationRepository registrations, ViewMapper views, AuditService audit,
                          ObjectMapper json, TransactionTemplate tx) {
    this.engine = engine;
    this.registrations = registrations;
    this.views = views;
    this.audit = audit;
    this.json = json;
    this.tx = tx;
  }

  @Transactional(readOnly = true)
  public List<TaskView> list(String assignee, String candidateGroup, UserTaskState state, Long processInstanceKey) {
    List<UserTask> found = engine.searchTasks(blankToNull(assignee), blankToNull(candidateGroup), state, processInstanceKey);
    Set<Long> keys = found.stream().map(UserTask::getProcessInstanceKey).collect(Collectors.toSet());
    Map<Long, Registration> byKey = keys.isEmpty() ? Map.of() : registrations.findByProcessInstanceKeyIn(keys).stream()
        .collect(Collectors.toMap(Registration::getProcessInstanceKey, Function.identity(), (a, b) -> a));
    return found.stream().map(t -> toView(t, byKey.get(t.getProcessInstanceKey()))).toList();
  }

  @Transactional(readOnly = true)
  public TaskDetailView detail(long userTaskKey) {
    UserTask task = engine.getTask(userTaskKey);
    Registration r = registrations.findByProcessInstanceKey(task.getProcessInstanceKey()).orElse(null);
    Map<String, String> vars = engine.processVariables(task.getProcessInstanceKey());
    Map<String, Object> form = null;
    Object schema = engine.taskFormSchema(userTaskKey);
    if (schema instanceof String text) {
      form = parse(text, new TypeReference<Map<String, Object>>() {});
    } else if (schema != null) {
      form = json.convertValue(schema, new TypeReference<Map<String, Object>>() {});
    }
    return new TaskDetailView(toView(task, r), parse(vars.get("memberChecks"), new TypeReference<List<Object>>() {}),
        parse(vars.get("sla"), new TypeReference<Map<String, Object>>() {}), form);
  }

  public TaskView assign(long userTaskKey, String assignee) {
    engine.assignTask(userTaskKey, assignee);
    return toView(engine.getTask(userTaskKey), null);
  }

  public void unassign(long userTaskKey) {
    engine.unassignTask(userTaskKey);
  }

  /** Validates the decision, completes the Camunda user task and records the decision in the read model. */
  public void complete(long userTaskKey, CompleteTaskRequest request) {
    UserTask task = engine.getTask(userTaskKey);
    if (task.getState() != UserTaskState.CREATED) {
      throw ApiException.conflict("This task is already " + task.getState().name().toLowerCase());
    }
    if (task.getAssignee() != null && !task.getAssignee().equals(request.actorId())) {
      throw ApiException.forbidden("This task is assigned to " + task.getAssignee());
    }
    String kind = kind(task.getElementId());
    Map<String, Object> variables = new HashMap<>();
    variables.put("decidedBy", request.actorId());
    variables.put("decidedAt", OffsetDateTime.now().toString());
    String decision;
    String remarks;
    if ("MANUAL_VERIFICATION".equals(kind)) {
      if (request.manuallyVerified() == null) {
        throw ApiException.badRequest("Say whether the members could be verified");
      }
      if (!request.manuallyVerified() && (request.invalidReason() == null || request.invalidReason().trim().length() < 10)) {
        throw ApiException.badRequest("Give a reason (at least 10 characters) when the registration cannot be verified");
      }
      variables.put("manuallyVerified", request.manuallyVerified());
      if (!request.manuallyVerified()) {
        variables.put("invalidReason", "Not verified by the Event Coordinator: " + request.invalidReason().trim());
      }
      decision = request.manuallyVerified() ? "VERIFIED" : "NOT_VERIFIED";
      remarks = request.invalidReason();
    } else {
      if (request.decision() == null) {
        throw ApiException.badRequest("Choose Approve or Reject");
      }
      if (ProcessConstants.DECISION_REJECTED.equals(request.decision())
          && (request.remarks() == null || request.remarks().trim().length() < 10)) {
        throw ApiException.badRequest("Remarks of at least 10 characters are required when rejecting, so the team knows what to fix");
      }
      variables.put("decision", request.decision());
      variables.put("faRemarks", request.remarks() == null ? "" : request.remarks().trim());
      variables.put("escalated", "ESCALATED_REVIEW".equals(kind));
      decision = request.decision();
      remarks = request.remarks();
    }
    if (task.getAssignee() == null) {
      engine.assignTask(userTaskKey, request.actorId());
    }
    // Record the decision before completing the task: once completed, workers update the same registration
    tx.executeWithoutResult(s -> registrations.findByProcessInstanceKey(task.getProcessInstanceKey()).ifPresent(r -> {
      if (!"MANUAL_VERIFICATION".equals(kind)) {
        r.setDecision(decision);
        r.setDecidedBy(request.actorId());
        r.setDecisionRemarks(remarks);
        r.setDecidedAt(OffsetDateTime.now());
      }
      audit.record(r.getId(), r.getEvent().getId(), task.getProcessInstanceKey(), "TASK_COMPLETED",
          humanKind(kind) + " completed: " + decision.toLowerCase().replace('_', ' '),
          remarks == null || remarks.isBlank() ? null : remarks, request.actorId(),
          decision.equals("APPROVED") || decision.equals("VERIFIED") ? "success" : "warning");
    }));
    engine.completeTask(userTaskKey, variables);
  }

  TaskView toView(UserTask t, Registration r) {
    return new TaskView(t.getUserTaskKey(), t.getName(), t.getElementId(), kind(t.getElementId()),
        t.getState() == null ? null : t.getState().name(), t.getAssignee(), t.getCandidateGroups(), t.getCreationDate(),
        t.getCompletionDate(), t.getDueDate(), t.getProcessInstanceKey(), t.getBpmnProcessId(), formId(t.getElementId()),
        r == null ? null : views.registration(r));
  }

  static String kind(String elementId) {
    return switch (elementId) {
      case ProcessConstants.TASK_FA_REVIEW -> "FA_REVIEW";
      case ProcessConstants.TASK_COORDINATOR_REVIEW -> "ESCALATED_REVIEW";
      case ProcessConstants.TASK_MANUAL_VERIFICATION -> "MANUAL_VERIFICATION";
      default -> "OTHER";
    };
  }

  private static String formId(String elementId) {
    return switch (elementId) {
      case ProcessConstants.TASK_FA_REVIEW -> "fa-team-review";
      case ProcessConstants.TASK_COORDINATOR_REVIEW -> "coordinator-escalated-review";
      case ProcessConstants.TASK_MANUAL_VERIFICATION -> "manual-verification";
      default -> null;
    };
  }

  private static String humanKind(String kind) {
    return switch (kind) {
      case "FA_REVIEW" -> "FA review";
      case "ESCALATED_REVIEW" -> "Escalated review";
      case "MANUAL_VERIFICATION" -> "Manual verification";
      default -> "Task";
    };
  }

  private <T> T parse(String value, TypeReference<T> type) {
    if (value == null) {
      return null;
    }
    try {
      return json.readValue(value, type);
    } catch (Exception e) {
      return null;
    }
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s;
  }
}
