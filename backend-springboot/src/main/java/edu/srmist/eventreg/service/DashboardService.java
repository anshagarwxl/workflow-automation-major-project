package edu.srmist.eventreg.service;

import edu.srmist.eventreg.domain.PoolStatus;
import edu.srmist.eventreg.domain.RegistrationStatus;
import edu.srmist.eventreg.repository.*;
import edu.srmist.eventreg.web.dto.Views.DashboardView;
import edu.srmist.eventreg.workflow.WorkflowEngine;
import io.camunda.client.api.search.enums.UserTaskState;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

  private final RegistrationRepository registrations;
  private final ApprovedTeamRepository approvedTeams;
  private final PoolEntryRepository pool;
  private final WorkflowEventRepository workflowEvents;
  private final EventRepository events;
  private final WorkflowEngine engine;
  private final ViewMapper views;

  public DashboardService(RegistrationRepository registrations, ApprovedTeamRepository approvedTeams, PoolEntryRepository pool,
      WorkflowEventRepository workflowEvents, EventRepository events, WorkflowEngine engine, ViewMapper views) {
    this.registrations = registrations;
    this.approvedTeams = approvedTeams;
    this.pool = pool;
    this.workflowEvents = workflowEvents;
    this.events = events;
    this.engine = engine;
    this.views = views;
  }

  @Transactional(readOnly = true)
  public DashboardView summary() {
    Map<String, Long> byStatus = new LinkedHashMap<>();
    for (RegistrationStatus s : RegistrationStatus.values()) {
      byStatus.put(s.name(), registrations.countByStatus(s));
    }
    Map<String, Object> engineInfo = engine.topology();
    long openTasks = 0;
    long incidents = 0;
    if (Boolean.TRUE.equals(engineInfo.get("connected"))) {
      try {
        openTasks = engine.searchTasks(null, null, UserTaskState.CREATED, null).size();
        incidents = engine.activeIncidents().size();
      } catch (RuntimeException e) {
        engineInfo.put("searchError", e.getMessage());
      }
    }
    return new DashboardView(byStatus, approvedTeams.count(), pool.countByStatus(PoolStatus.WAITING), openTasks, incidents,
        events.findAllByOrderByRegistrationDeadlineAsc().stream().map(views::event).toList(),
        workflowEvents.findTop25ByOrderByOccurredAtDescIdDesc().stream().map(views::timeline).toList(), engineInfo);
  }
}
