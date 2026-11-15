package edu.srmist.eventreg.workflow;

import edu.srmist.eventreg.config.WorkflowProperties;
import edu.srmist.eventreg.workflow.Payloads.SlaSettings;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

/**
 * Supplies the {@code sla} variable for new process instances. The STANDARD preset holds the real business
 * values (24 h reminder, 72 h escalation); the DEMO preset shortens them so timers can be shown live.
 */
@Service
public class SlaService {

  public enum Preset { STANDARD, DEMO }

  private final WorkflowProperties properties;
  private final AtomicReference<Preset> preset;

  public SlaService(WorkflowProperties properties) {
    this.properties = properties;
    this.preset = new AtomicReference<>(Preset.valueOf(properties.getSla().getPreset().toUpperCase()));
  }

  public Preset preset() {
    return preset.get();
  }

  public void use(Preset newPreset) {
    preset.set(newPreset);
  }

  public SlaSettings current() {
    WorkflowProperties.Durations d = preset.get() == Preset.DEMO ? properties.getSla().getDemo() : properties.getSla().getStandard();
    return new SlaSettings(d.getVerificationTimeout(), d.getFaReminderAfter(), d.getFaEscalationAfter());
  }
}
