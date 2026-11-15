package edu.srmist.eventreg.web;

import edu.srmist.eventreg.service.EventService;
import edu.srmist.eventreg.web.dto.Requests.CreateEventRequest;
import edu.srmist.eventreg.web.dto.Views.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Events", description = "Events, unallocated pool, approved teams and random allocation")
public class EventController {

  private final EventService events;

  public EventController(EventService events) {
    this.events = events;
  }

  @GetMapping
  public List<EventView> list() {
    return events.list();
  }

  @GetMapping("/{id}")
  public EventView get(@PathVariable Long id) {
    return events.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EventView create(@Valid @RequestBody CreateEventRequest request) {
    return events.create(request);
  }

  @PostMapping("/{id}/close")
  @Operation(summary = "Close registration: publishes the 'registration-closed' message that starts Random Team Allocation")
  public EventView close(@PathVariable Long id, @RequestParam(required = false) String actor) {
    return events.closeRegistration(id, actor);
  }

  @GetMapping("/{id}/pool")
  public List<PoolEntryView> pool(@PathVariable Long id) {
    return events.pool(id);
  }

  @GetMapping("/{id}/approved-teams")
  public List<ApprovedTeamView> approvedTeams(@PathVariable Long id) {
    return events.approvedTeams(id);
  }

  @GetMapping("/{id}/allocation")
  @Operation(summary = "Random Team Allocation status: rounds, random teams, pool and the live process tracker")
  public AllocationView allocation(@PathVariable Long id) {
    return events.allocation(id);
  }
}
