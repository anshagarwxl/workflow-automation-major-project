package edu.srmist.eventreg;

import io.camunda.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Event Team Registration backend. On startup it deploys the BPMN models and Camunda forms from the
 * repository's bpmn-models folder (packaged on the classpath under /bpmn) and opens the job workers.
 */
@SpringBootApplication
@Deployment(resources = {"classpath*:bpmn/**/*.form", "classpath*:bpmn/**/*.bpmn"})
public class EventRegistrationApplication {

  public static void main(String[] args) {
    SpringApplication.run(EventRegistrationApplication.class, args);
  }
}
