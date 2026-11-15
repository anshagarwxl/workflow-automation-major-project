package edu.srmist.eventreg.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Externalised workflow configuration ({@code workflow.*} in application.yml). */
@ConfigurationProperties(prefix = "workflow")
@Getter
@Setter
public class WorkflowProperties {

  private Sla sla = new Sla();

  /** Shown in the portal so testers can open the captured emails. */
  private String mailboxUrl = "http://localhost:8025";

  /** Camunda Operate URL, shown as deep links from the tracker. */
  private String operateUrl = "http://localhost:8080/operate";

  @Getter
  @Setter
  public static class Sla {
    private String preset = "STANDARD";
    private Durations standard = new Durations("PT10M", "PT24H", "PT72H");
    private Durations demo = new Durations("PT1M", "PT1M", "PT3M");
  }

  @Getter
  @Setter
  public static class Durations {
    private String verificationTimeout;
    private String faReminderAfter;
    private String faEscalationAfter;

    public Durations() {}

    public Durations(String verificationTimeout, String faReminderAfter, String faEscalationAfter) {
      this.verificationTimeout = verificationTimeout;
      this.faReminderAfter = faReminderAfter;
      this.faEscalationAfter = faEscalationAfter;
    }
  }
}
