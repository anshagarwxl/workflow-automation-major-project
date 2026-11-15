package edu.srmist.eventreg.web;

import edu.srmist.eventreg.domain.RegistrationStatus;
import edu.srmist.eventreg.repository.NotificationLogRepository;
import edu.srmist.eventreg.service.RegistrationService;
import edu.srmist.eventreg.service.TrackerService;
import edu.srmist.eventreg.service.ViewMapper;
import edu.srmist.eventreg.web.dto.Requests.CreateRegistrationRequest;
import edu.srmist.eventreg.web.dto.Views.NotificationView;
import edu.srmist.eventreg.web.dto.Views.RegistrationView;
import edu.srmist.eventreg.web.dto.Views.TrackerView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/registrations")
@Tag(name = "Registrations", description = "Submit registrations and follow them through the workflow")
public class RegistrationController {

  private final RegistrationService service;
  private final TrackerService tracker;
  private final NotificationLogRepository notifications;
  private final ViewMapper views;

  public RegistrationController(RegistrationService service, TrackerService tracker, NotificationLogRepository notifications,
                                ViewMapper views) {
    this.service = service;
    this.tracker = tracker;
    this.notifications = notifications;
    this.views = views;
  }

  @PostMapping
  @Operation(summary = "Submit a team or individual registration; starts an Event Team Registration process instance")
  public ResponseEntity<RegistrationView> submit(@Valid @RequestBody CreateRegistrationRequest request) {
    RegistrationView created = service.submit(request);
    return ResponseEntity.created(URI.create("/api/registrations/" + created.id())).body(created);
  }

  @GetMapping
  @Operation(summary = "List registrations, optionally filtered by event, status or member registration number")
  public List<RegistrationView> list(@RequestParam(required = false) Long eventId,
                                     @RequestParam(required = false) RegistrationStatus status,
                                     @RequestParam(required = false) String regNo) {
    return service.list(eventId, status, regNo);
  }

  @GetMapping("/{id}")
  public RegistrationView get(@PathVariable String id) {
    return service.get(id);
  }

  @GetMapping("/{id}/tracker")
  @Operation(summary = "Live status: engine element instances, taken flows, incidents, open tasks and audit timeline")
  public TrackerView tracker(@PathVariable String id) {
    return tracker.forRegistration(id);
  }

  @GetMapping("/{id}/notifications")
  public List<NotificationView> notifications(@PathVariable String id) {
    return notifications.findByRegistrationIdOrderBySentAtDesc(id).stream().map(views::notification).toList();
  }
}
