package edu.srmist.eventreg.web;

import edu.srmist.eventreg.service.DashboardService;
import edu.srmist.eventreg.service.TrackerService;
import edu.srmist.eventreg.web.dto.Views.DashboardView;
import edu.srmist.eventreg.web.dto.Views.TrackerView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Process monitoring", description = "Dashboard, process instance tracker and BPMN XML")
public class ProcessController {

  private final TrackerService tracker;
  private final DashboardService dashboard;

  public ProcessController(TrackerService tracker, DashboardService dashboard) {
    this.tracker = tracker;
    this.dashboard = dashboard;
  }

  @GetMapping("/dashboard")
  public DashboardView dashboard() {
    return dashboard.summary();
  }

  @GetMapping("/process-instances/{key}/tracker")
  public TrackerView tracker(@PathVariable long key) {
    return tracker.forProcessInstance(key, null);
  }

  @GetMapping(value = "/process-instances/{key}/xml", produces = MediaType.APPLICATION_XML_VALUE)
  @Operation(summary = "BPMN XML of the exact process definition version the instance runs on")
  public String xml(@PathVariable long key) {
    return tracker.processXml(key);
  }
}
