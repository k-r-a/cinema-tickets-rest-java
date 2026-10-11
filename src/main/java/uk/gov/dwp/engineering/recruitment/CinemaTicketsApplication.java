package uk.gov.dwp.engineering.recruitment;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@OpenAPIDefinition(info = @Info(title = "Cinema Tickets Booking Service",
description = "This application validates ticket requests, processes payments and reserves seats"))
public class CinemaTicketsApplication {

  public static void main(String[] args) {
    SpringApplication.run(CinemaTicketsApplication.class, args);
  }

}
