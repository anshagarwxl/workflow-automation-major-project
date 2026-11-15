package edu.srmist.eventreg.config;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/**
 * Fault-injection switches used to demonstrate the failure paths on demand (chaos testing):
 * a Student Records outage (drives the verification timer + incident path) and a forced member conflict
 * on the next approval (drives the MEMBER_CONFLICT error boundary).
 */
@Component
public class SimulationSettings {

  private final AtomicBoolean studentRecordsOutage = new AtomicBoolean(false);
  private final AtomicBoolean forceMemberConflict = new AtomicBoolean(false);

  public boolean studentRecordsOutage() {
    return studentRecordsOutage.get();
  }

  public void setStudentRecordsOutage(boolean value) {
    studentRecordsOutage.set(value);
  }

  public boolean forceMemberConflict() {
    return forceMemberConflict.get();
  }

  public void setForceMemberConflict(boolean value) {
    forceMemberConflict.set(value);
  }

  /** Returns true once, then resets: the conflict applies to a single approval. */
  public boolean consumeForcedConflict() {
    return forceMemberConflict.getAndSet(false);
  }
}
