package edu.srmist.eventreg.service;

import edu.srmist.eventreg.domain.Registration;
import edu.srmist.eventreg.config.WorkflowProperties;
import edu.srmist.eventreg.repository.NotificationLogRepository;
import edu.srmist.eventreg.repository.RegistrationRepository;
import edu.srmist.eventreg.repository.WorkflowEventRepository;
import edu.srmist.eventreg.web.ApiException;
import edu.srmist.eventreg.web.dto.Views.*;
import edu.srmist.eventreg.workflow.WorkflowEngine;
import io.camunda.client.api.search.enums.ElementInstanceState;
import io.camunda.client.api.search.enums.ElementInstanceType;
import io.camunda.client.api.search.enums.UserTaskState;
import io.camunda.client.api.search.response.ElementInstance;
import io.camunda.client.api.search.response.ProcessInstance;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Live process status tracker: combines the engine's runtime state (element instances, taken sequence flows,
 * incidents, child instances, open user tasks) with the portal's audit trail and notification log.
 */
@Service
public class TrackerService {

  private static final Logger log = LoggerFactory.getLogger(TrackerService.class);

  private final WorkflowEngine engine;
  private final RegistrationRepository registrations;
  private final WorkflowEventRepository workflowEvents;
  private final NotificationLogRepository notifications;
  private final ViewMapper views;
  private final TaskInboxService tasks;
  private final WorkflowProperties properties;

  public TrackerService(WorkflowEngine engine, RegistrationRepository registrations, WorkflowEventRepository workflowEvents,
      NotificationLogRepository notifications, ViewMapper views, TaskInboxService tasks, WorkflowProperties properties) {
    this.engine = engine;
    this.registrations = registrations;
    this.workflowEvents = workflowEvents;
    this.notifications = notifications;
    this.views = views;
    this.tasks = tasks;
    this.properties = properties;
  }

  @Transactional(readOnly = true)
  public TrackerView forRegistration(String registrationId) {
    Registration r = registrations.findById(registrationId)
        .orElseThrow(() -> ApiException.notFound("Registration " + registrationId));
    if (r.getProcessInstanceKey() == null) {
      return empty(r);
    }
    return forProcessInstance(r.getProcessInstanceKey(), r);
  }

  @Transactional(readOnly = true)
  public TrackerView forProcessInstance(long processInstanceKey, Registration registration) {
    Optional<ProcessInstance> instance = engine.findProcessInstance(processInstanceKey);
    if (registration == null) {
      registration = registrations.findByProcessInstanceKey(processInstanceKey).orElse(null);
    }
    List<ElementInstance> elements = safe(() -> engine.elementInstances(processInstanceKey), List.of());
    Set<String> active = new LinkedHashSet<>();
    Set<String> completed = new LinkedHashSet<>();
    Set<String> incident = new LinkedHashSet<>();
    List<ElementView> elementViews = new ArrayList<>();
    for (ElementInstance e : elements) {
      if (e.getType() == ElementInstanceType.PROCESS) {
        continue;
      }
      if (e.getState() == ElementInstanceState.ACTIVE) {
        active.add(e.getElementId());
      } else if (e.getState() == ElementInstanceState.COMPLETED) {
        completed.add(e.getElementId());
      }
      if (Boolean.TRUE.equals(e.getIncident())) {
        incident.add(e.getElementId());
      }
      if (e.getType() != ElementInstanceType.MULTI_INSTANCE_BODY) {
        elementViews.add(new ElementView(e.getElementId(), e.getElementName(), e.getType().name(), e.getState().name(),
            e.getStartDate(), e.getEndDate(), Boolean.TRUE.equals(e.getIncident())));
      }
    }
    List<IncidentView> incidents = safe(() -> engine.incidents(processInstanceKey), List.<io.camunda.client.api.search.response.Incident>of())
        .stream()
        .map(i -> new IncidentView(i.getIncidentKey(), i.getElementId(), i.getErrorType() == null ? null : i.getErrorType().name(),
            i.getErrorMessage(), i.getCreationTime(), i.getState() == null ? null : i.getState().name()))
        .toList();
    incidents.stream().filter(i -> "ACTIVE".equals(i.state())).forEach(i -> incident.add(i.elementId()));

    List<InstanceView> children = safe(() -> engine.childInstances(processInstanceKey), List.<ProcessInstance>of())
        .stream().map(TrackerService::instanceView).toList();
    List<TaskView> openTasks = safe(() -> tasks.list(null, null, UserTaskState.CREATED, processInstanceKey), List.of());

    List<TimelineEntry> timeline;
    List<NotificationView> notes = List.of();
    RegistrationView regView = null;
    if (registration != null) {
      timeline = workflowEvents.findByRegistrationIdOrderByOccurredAtAscIdAsc(registration.getId()).stream()
          .map(views::timeline).toList();
      notes = notifications.findByRegistrationIdOrderBySentAtDesc(registration.getId()).stream().map(views::notification).toList();
      regView = views.registration(registration);
    } else {
      timeline = instance.map(pi -> workflowEvents.findAll().stream()
              .filter(w -> Objects.equals(w.getProcessInstanceKey(), processInstanceKey))
              .sorted(Comparator.comparing(edu.srmist.eventreg.domain.WorkflowEvent::getOccurredAt))
              .map(views::timeline).toList())
          .orElse(List.of());
    }
    completed.removeAll(active);
    return new TrackerView(instance.map(TrackerService::instanceView).orElse(null), new ArrayList<>(active),
        new ArrayList<>(completed), new ArrayList<>(incident), safe(() -> engine.takenSequenceFlows(processInstanceKey), List.of()),
        elementViews, incidents, children, openTasks, timeline, notes, regView, properties.getOperateUrl());
  }

  public String processXml(long processInstanceKey) {
    ProcessInstance pi = engine.findProcessInstance(processInstanceKey)
        .orElseThrow(() -> ApiException.notFound("Process instance " + processInstanceKey));
    return engine.processXml(pi.getProcessDefinitionKey());
  }

  private TrackerView empty(Registration r) {
    return new TrackerView(null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
        workflowEvents.findByRegistrationIdOrderByOccurredAtAscIdAsc(r.getId()).stream().map(views::timeline).toList(),
        List.of(), views.registration(r), properties.getOperateUrl());
  }

  static InstanceView instanceView(ProcessInstance pi) {
    return new InstanceView(pi.getProcessInstanceKey(), pi.getProcessDefinitionId(), pi.getProcessDefinitionName(),
        pi.getProcessDefinitionVersion(), pi.getProcessDefinitionKey(), pi.getState() == null ? null : pi.getState().name(),
        Boolean.TRUE.equals(pi.getHasIncident()), pi.getStartDate(), pi.getEndDate(), pi.getParentProcessInstanceKey());
  }

  private static <T> T safe(java.util.function.Supplier<T> supplier, T fallback) {
    try {
      return supplier.get();
    } catch (RuntimeException e) {
      log.debug("Tracker query failed: {}", e.getMessage());
      return fallback;
    }
  }
}
