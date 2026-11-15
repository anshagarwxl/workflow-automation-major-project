package edu.srmist.eventreg.web;

import edu.srmist.eventreg.config.SimulationSettings;
import edu.srmist.eventreg.config.WorkflowProperties;
import edu.srmist.eventreg.web.dto.Requests.SettingsRequest;
import edu.srmist.eventreg.web.dto.Views.SettingsView;
import edu.srmist.eventreg.workflow.SlaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/settings")
@Tag(name = "Admin", description = "SLA preset and fault-injection switches for demonstrating failure handling")
public class AdminController {

  private final SlaService sla;
  private final SimulationSettings simulation;
  private final WorkflowProperties properties;

  public AdminController(SlaService sla, SimulationSettings simulation, WorkflowProperties properties) {
    this.sla = sla;
    this.simulation = simulation;
    this.properties = properties;
  }

  @GetMapping
  public SettingsView get() {
    return view();
  }

  @PutMapping
  @Operation(summary = "Switch the SLA preset (STANDARD/DEMO) or toggle the simulated failures")
  public SettingsView update(@RequestBody SettingsRequest request) {
    if (request.slaPreset() != null) {
      try {
        sla.use(SlaService.Preset.valueOf(request.slaPreset().toUpperCase()));
      } catch (IllegalArgumentException e) {
        throw ApiException.badRequest("slaPreset must be STANDARD or DEMO");
      }
    }
    if (request.studentRecordsOutage() != null) {
      simulation.setStudentRecordsOutage(request.studentRecordsOutage());
    }
    if (request.forceMemberConflict() != null) {
      simulation.setForceMemberConflict(request.forceMemberConflict());
    }
    return view();
  }

  private SettingsView view() {
    return new SettingsView(sla.preset().name(), sla.current(), simulation.studentRecordsOutage(),
        simulation.forceMemberConflict(), properties.getMailboxUrl(), properties.getOperateUrl());
  }
}
