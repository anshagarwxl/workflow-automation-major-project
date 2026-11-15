package edu.srmist.eventreg.workflow;

import edu.srmist.eventreg.domain.Event;
import edu.srmist.eventreg.workflow.Payloads.RegistrationPayload;
import edu.srmist.eventreg.workflow.Payloads.SlaSettings;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.response.PublishMessageResponse;
import io.camunda.client.api.search.enums.UserTaskState;
import io.camunda.client.api.search.response.ElementInstance;
import io.camunda.client.api.search.response.Incident;
import io.camunda.client.api.search.response.ProcessInstance;
import io.camunda.client.api.search.response.ProcessInstanceSequenceFlow;
import io.camunda.client.api.search.response.UserTask;
import io.camunda.client.api.search.response.Variable;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The only class that talks to the Camunda 8 Orchestration Cluster. Commands (start, publish, assign,
 * complete) go through the gateway; queries (user tasks, element instances, incidents, variables) use the
 * Orchestration Cluster REST search API.
 */
@Component
public class WorkflowEngine {

  private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);
  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  private final CamundaClient client;
  private final Map<Long, String> xmlCache = new ConcurrentHashMap<>();

  public WorkflowEngine(CamundaClient client) {
    this.client = client;
  }

  // ------------------------------------------------------------------ commands

  public long startRegistration(RegistrationPayload registration, SlaSettings sla, String coordinatorEmail) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("registration", registration);
    variables.put("sla", sla);
    variables.put("coordinatorEmail", coordinatorEmail);
    ProcessInstanceEvent instance = client.newCreateInstanceCommand()
        .bpmnProcessId(ProcessConstants.PROCESS_REGISTRATION)
        .latestVersion()
        .variables(variables)
        .tags("registration:" + registration.registrationId(), "event:" + registration.eventId())
        .requestTimeout(TIMEOUT)
        .send()
        .join();
    log.info("Started {} for registration {} -> process instance {}", ProcessConstants.PROCESS_REGISTRATION,
        registration.registrationId(), instance.getProcessInstanceKey());
    return instance.getProcessInstanceKey();
  }

  /** Publishes 'registration-closed', which starts Random Team Allocation for the event. Idempotent per event. */
  public long publishRegistrationClosed(Event event, SlaSettings sla) {
    Map<String, Object> variables = new LinkedHashMap<>();
    variables.put("eventId", event.getId());
    variables.put("eventName", event.getName());
    variables.put("minTeamSize", event.getMinTeamSize());
    variables.put("maxTeamSize", event.getMaxTeamSize());
    variables.put("maxAllocationRounds", event.getMaxAllocationRounds());
    variables.put("allocationRound", 0);
    variables.put("sla", sla);
    variables.put("coordinatorEmail", event.getCoordinatorEmail());
    PublishMessageResponse response = client.newPublishMessageCommand()
        .messageName(ProcessConstants.MESSAGE_REGISTRATION_CLOSED)
        .correlationKey(String.valueOf(event.getId()))
        .messageId("registration-closed-" + event.getId())
        .timeToLive(Duration.ofMinutes(10))
        .variables(variables)
        .requestTimeout(TIMEOUT)
        .send()
        .join();
    log.info("Published {} for event {} (message key {})", ProcessConstants.MESSAGE_REGISTRATION_CLOSED, event.getCode(),
        response.getMessageKey());
    return response.getMessageKey();
  }

  public void assignTask(long userTaskKey, String assignee) {
    client.newAssignUserTaskCommand(userTaskKey).assignee(assignee).allowOverride(true)
        .requestTimeout(TIMEOUT).send().join();
  }

  public void unassignTask(long userTaskKey) {
    client.newUnassignUserTaskCommand(userTaskKey).requestTimeout(TIMEOUT).send().join();
  }

  public void completeTask(long userTaskKey, Map<String, Object> variables) {
    client.newCompleteUserTaskCommand(userTaskKey).variables(variables).requestTimeout(TIMEOUT).send().join();
  }

  // ------------------------------------------------------------------ queries

  public List<UserTask> searchTasks(String assignee, String candidateGroup, UserTaskState state, Long processInstanceKey) {
    return client.newUserTaskSearchRequest()
        .filter(f -> {
          if (assignee != null) {
            f.assignee(assignee);
          }
          if (candidateGroup != null) {
            f.candidateGroup(candidateGroup);
          }
          if (state != null) {
            f.state(state);
          }
          if (processInstanceKey != null) {
            f.processInstanceKey(processInstanceKey);
          }
        })
        .sort(s -> s.creationDate().desc())
        .page(p -> p.limit(200))
        .send()
        .join()
        .items();
  }

  public UserTask getTask(long userTaskKey) {
    return client.newUserTaskGetRequest(userTaskKey).send().join();
  }

  public Optional<ProcessInstance> findProcessInstance(long processInstanceKey) {
    try {
      return Optional.of(client.newProcessInstanceGetRequest(processInstanceKey).send().join());
    } catch (RuntimeException e) {
      // Not exported to secondary storage yet (eventual consistency) or unknown key
      log.debug("Process instance {} not found yet: {}", processInstanceKey, e.getMessage());
      return Optional.empty();
    }
  }

  public List<ProcessInstance> childInstances(long parentProcessInstanceKey) {
    return client.newProcessInstanceSearchRequest()
        .filter(f -> f.parentProcessInstanceKey(parentProcessInstanceKey))
        .sort(s -> s.startDate().asc())
        .page(p -> p.limit(200))
        .send()
        .join()
        .items();
  }

  public List<ElementInstance> elementInstances(long processInstanceKey) {
    return client.newElementInstanceSearchRequest()
        .filter(f -> f.processInstanceKey(processInstanceKey))
        .sort(s -> s.startDate().asc())
        .page(p -> p.limit(1000))
        .send()
        .join()
        .items();
  }

  public List<Incident> incidents(long processInstanceKey) {
    return client.newIncidentSearchRequest()
        .filter(f -> f.processInstanceKey(processInstanceKey))
        .page(p -> p.limit(100))
        .send()
        .join()
        .items();
  }

  public List<String> takenSequenceFlows(long processInstanceKey) {
    try {
      return client.newProcessInstanceSequenceFlowsRequest(processInstanceKey).send().join().stream()
          .map(ProcessInstanceSequenceFlow::getElementId)
          .distinct()
          .toList();
    } catch (RuntimeException e) {
      return List.of();
    }
  }

  /** Process-level variables of an instance as raw JSON strings, keyed by name. */
  public Map<String, String> processVariables(long processInstanceKey) {
    List<Variable> variables = client.newVariableSearchRequest()
        .filter(f -> f.processInstanceKey(processInstanceKey).scopeKey(processInstanceKey))
        .page(p -> p.limit(200))
        .send()
        .join()
        .items();
    Map<String, String> result = new LinkedHashMap<>();
    for (Variable v : variables) {
      String value = Boolean.TRUE.equals(v.isTruncated())
          ? client.newVariableGetRequest(v.getVariableKey()).send().join().getValue()
          : v.getValue();
      result.put(v.getName(), value);
    }
    return result;
  }

  /** The linked Camunda form (JSON schema) of a user task, or null when it has none. */
  public Object taskFormSchema(long userTaskKey) {
    try {
      return client.newUserTaskGetFormRequest(userTaskKey).send().join().getSchema();
    } catch (RuntimeException e) {
      return null;
    }
  }

  public List<Incident> activeIncidents() {
    return client.newIncidentSearchRequest()
        .filter(f -> f.state(io.camunda.client.api.search.enums.IncidentState.ACTIVE))
        .page(p -> p.limit(200))
        .send()
        .join()
        .items();
  }

  /** Cluster topology, used by the dashboard's engine health indicator. */
  public Map<String, Object> topology() {
    Map<String, Object> result = new LinkedHashMap<>();
    try {
      var topology = client.newTopologyRequest().requestTimeout(Duration.ofSeconds(3)).send().join();
      result.put("connected", true);
      result.put("gatewayVersion", topology.getGatewayVersion());
      result.put("brokers", topology.getBrokers().size());
      result.put("partitions", topology.getPartitionsCount());
    } catch (RuntimeException e) {
      result.put("connected", false);
      result.put("error", e.getMessage());
    }
    return result;
  }

  public String processXml(long processDefinitionKey) {
    return xmlCache.computeIfAbsent(processDefinitionKey,
        key -> client.newProcessDefinitionGetXmlRequest(key).send().join());
  }
}
