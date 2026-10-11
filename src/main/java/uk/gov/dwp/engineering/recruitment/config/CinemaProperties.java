package uk.gov.dwp.engineering.recruitment.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/** Cinema business configuration bound from {@code cinem.*} and validated at startup */
@Validated
@ConfigurationProperties(prefix = "cinema")
public record CinemaProperties(@NotNull @Valid Tickets tickets, @NotNull @Valid Booking booking) {

    /** Bound from {@code cinema.tickets.categories.*}. */
    public record Tickets(@NotNull @Valid Map<TicketType, @NotNull @Valid Category> categories) {

        @AssertTrue(message = "A category must be configured for every ticket type")
        public boolean isEveryTicketTypeConfigured() {
            return categories == null || categories.keySet().containsAll(Set.of(TicketType.values()));
        }
    }

    /** Configuration of one ticket type to specify if seat is allocated. */
    public record Category(@NotNull @PositiveOrZero BigDecimal price, @DefaultValue("true") boolean occupiesSeat) {
    }

    /** Bound from {@code cinema.booking.*}. */
    public record Booking(@Positive int maxTicketsPerBooking) {
    }

    public BigDecimal priceOf(final TicketType type) { return tickets.categories().get(type).price(); }

    public boolean occupiesSeat(final TicketType type) { return tickets.categories().get(type).occupiesSeat(); }

    public BigDecimal costOf(final TicketType type, final long ticketCount) {
        return priceOf(type).multiply(BigDecimal.valueOf(ticketCount));
    }

    public int maxTicketsPerBooking() { return booking.maxTicketsPerBooking(); }
}
