package edu.srmist.eventreg.web;

import edu.srmist.eventreg.service.TaskInboxService;
import edu.srmist.eventreg.web.dto.Requests.AssignTaskRequest;
import edu.srmist.eventreg.web.dto.Requests.CompleteTaskRequest;
import edu.srmist.eventreg.web.dto.Views.TaskDetailView;
import edu.srmist.eventreg.web.dto.Views.TaskView;
import io.camunda.client.api.search.enums.UserTaskState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasklist", description = "Custom Tasklist inbox on top of Camunda user tasks")
public class TaskController {

  private final TaskInboxService tasks;

  public TaskController(TaskInboxService tasks) {
    this.tasks = tasks;
  }

  @GetMapping
  @Operation(summary = "Search user tasks by assignee and/or candidate group (state CREATED by default)")
  public List<TaskView> list(@RequestParam(required = false) String assignee,
                             @RequestParam(required = false) String candidateGroup,
                             @RequestParam(defaultValue = "CREATED") String state) {
    UserTaskState s = "ALL".equalsIgnoreCase(state) ? null : UserTaskState.valueOf(state.toUpperCase());
    return tasks.list(assignee, candidateGroup, s, null);
  }

  @GetMapping("/{key}")
  public TaskDetailView detail(@PathVariable long key) {
    return tasks.detail(key);
  }

  @PostMapping("/{key}/assignment")
  @Operation(summary = "Claim (assign) a task")
  public TaskView assign(@PathVariable long key, @Valid @RequestBody AssignTaskRequest request) {
    return tasks.assign(key, request.assignee());
  }

  @DeleteMapping("/{key}/assignment")
  @Operation(summary = "Unclaim a task")
  public ResponseEntity<Void> unassign(@PathVariable long key) {
    tasks.unassign(key);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{key}/completion")
  @Operation(summary = "Complete a task with a decision (FA / escalated review) or a verification result")
  public ResponseEntity<Void> complete(@PathVariable long key, @Valid @RequestBody CompleteTaskRequest request) {
    tasks.complete(key, request);
    return ResponseEntity.noContent().build();
  }
}
