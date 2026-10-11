package uk.gov.dwp.engineering.recruitment.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.CHILD;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;

import java.math.BigDecimal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class CinemaPropertiesTest {

    private final ApplicationContextRunner contextRunner =
    new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class) ;

    @Autowired
    private CinemaProperties configuredProperties;

    @Test
    void givenApplicationProperties_whenBound_thenPricesSeatingAndLimitAreLoaded() {
        assertEquals(new BigDecimal("25.99"), configuredProperties.priceOf(ADULT));
        assertEquals(new BigDecimal("17.50"), configuredProperties.priceOf(CHILD));
        assertEquals(0, BigDecimal.ZERO.compareTo(configuredProperties.priceOf(INFANT)));
        assertTrue(configuredProperties.occupiesSeat(ADULT));
        assertTrue(configuredProperties.occupiesSeat(CHILD));
        assertFalse(configuredProperties.occupiesSeat(INFANT));
        assertEquals(25, configuredProperties.maxTicketsPerBooking());
    }

    @Test
    void givenProperties_whenCostOf_thenPriceTimesCount() {
        assertEquals(new BigDecimal("52.50"), configuredProperties.costOf(CHILD,3));
    }

    @Test
    void givenNoProperties_whenBound_thenStartupFails() {
        contextRunner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void givenMissingType_whenBound_thenStartupFails() {
        contextRunner.withPropertyValues("cinema.booking.max-tickets-per-booking=25",
                        "cinema.tickets.categories.child.price=1", "cinema.tickets.categories.adult.price=1")
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .hasStackTraceContaining("A category must be configured for every ticket type"));
    }

    @Test
    void givenMissingPrice_whenBound_thenStartupFails() {
        contextRunner.withPropertyValues("cinema.booking.max-tickets-per-booking=25",
                        "cinema.tickets.categories.infant.price=0",
                        "cinema.tickets.categories.child.occupies-seat=true",
                        "cinema.tickets.categories.adult.price=1")
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .hasStackTraceContaining("price"));
    }

    @Test
    void givenNegativePrice_whenBound_thenStartupFails() {
        contextRunner.withPropertyValues("cinema.booking.max-tickets-per-booking=25",
                        "cinema.tickets.categories.infant.price=0", "cinema.tickets.categories.child.price=1",
                        "cinema.tickets.categories.adult.price=-0.01")
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .hasStackTraceContaining("price"));
    }

    @Test
    void givenMissingOccupiesSeat_whenBound_thenOccupiesSeatDefaultsToTrue() {
        contextRunner.withPropertyValues("cinema.booking.max-tickets-per-booking=25",
                        "cinema.tickets.categories.infant.price=0", "cinema.tickets.categories.child.price=1",
                        "cinema.tickets.categories.adult.price=2")
                .run(context ->
                        assertTrue(context.getBean(CinemaProperties.class).occupiesSeat(INFANT)));
    }

    @Test
    void givenMissingMaxTickets_whenBound_thenStartupFails() {
        contextRunner.withPropertyValues("cinema.tickets.categories.infant.price=0",
                        "cinema.tickets.categories.child.price=1", "cinema.tickets.categories.adult.price=2")
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .hasStackTraceContaining("booking"));
    }

    @Test
    void givenNonPositiveMaxTickets_whenBound_thenStartupFails() {
        contextRunner.withPropertyValues("cinema.booking.max-tickets-per-booking=0",
                        "cinema.tickets.categories.infant.price=0", "cinema.tickets.categories.child.price=1",
                        "cinema.tickets.categories.adult.price=2")
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .hasStackTraceContaining("maxTicketsPerBooking"));
    }

    @Configuration
    @EnableConfigurationProperties(CinemaProperties.class)
    static class PropertiesConfig {
    }
}
