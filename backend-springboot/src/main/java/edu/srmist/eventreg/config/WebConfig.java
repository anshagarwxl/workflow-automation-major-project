package edu.srmist.eventreg.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(WorkflowProperties.class)
public class WebConfig implements WebMvcConfigurer {

  @Value("${app.cors.allowed-origins:http://localhost:5173}")
  private String[] allowedOrigins;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**").allowedOrigins(allowedOrigins).allowedMethods("GET", "POST", "PUT", "DELETE");
  }

  @Bean
  OpenAPI openApi() {
    return new OpenAPI().info(new Info()
        .title("Event Team Registration API")
        .version("2.0.0")
        .description("REST API of the Event Team Registration portal. Business processes run on Camunda 8."));
  }
}
