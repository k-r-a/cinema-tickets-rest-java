package uk.gov.dwp.engineering.recruitment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.config.CinemaProperties;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

@Service
public class CinemaTicketsServiceImpl implements CinemaTicketsService {

    private static final Logger LOG = LoggerFactory.getLogger(CinemaTicketsServiceImpl.class);

    private final PaymentService paymentService;

    private final SeatReservationService seatReservationService;

    private final CinemaProperties properties;

    public CinemaTicketsServiceImpl(PaymentService paymentService, SeatReservationService seatReservationService,
                                    CinemaProperties properties) {
        this.paymentService = paymentService;
        this.seatReservationService = seatReservationService;
        this.properties = properties;
    }

    @Override
    public BookingConfirmation purchaseTickets(final Long accountId, TicketRequest... ticketRequests)
            throws InvalidBookingException {
        LOG.debug("Inside CinemaTicketsServiceImpl purchaseTickets");

        validateAccountId(accountId);
        final Map<TicketType, Long> counts = countTicketsByType(ticketRequests);
        validateBusinessRules(counts);

        final long seatCount = counts.entrySet().stream()
                .filter(entry -> properties.occupiesSeat(entry.getKey()))
                .mapToLong(Map.Entry::getValue)
                .sum();
        final BigDecimal totalCost = calculateTotalCost(counts);

        paymentService.debitAccount(accountId, totalCost);
        seatReservationService.reserveSeats(accountId, seatCount);
        LOG.info("Booking successful for account {}. paid {} for {} seats.", accountId, totalCost, seatCount);

        return new BookingConfirmation(accountId, seatCount, totalCost);
    }

    private BigDecimal calculateTotalCost(final Map<TicketType, Long> counts) {
        return counts.entrySet().stream()
                .map(entry -> properties.costOf(entry.getKey(), entry.getValue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static void validateAccountId(final Long accountId) {
        if(accountId == null || accountId <= 0) {
            throw new InvalidBookingException("Account id must be greater than zero");
        }
    }

    private Map<TicketType, Long> countTicketsByType(final TicketRequest... ticketRequests) {
        if(ticketRequests == null || ticketRequests.length == 0) {
            throw new InvalidBookingException("At least one ticket request is required");
        }

        final Map<TicketType, Long> counts = new EnumMap<>(TicketType.class);
        Arrays.stream(TicketType.values()).forEach(type -> counts.put(type, 0L));

        for(final TicketRequest request : ticketRequests) {
            if(request ==null || request.type() == null) {
                throw new InvalidBookingException("Each ticket request must specify a ticket type");
            }
            if(request.ticketCount() < 0) {
                throw new InvalidBookingException("Ticket count must not be negative");
            }
            counts.merge(request.type(), (long) request.ticketCount(), Long::sum);
        }
        return counts;
    }

    private void validateBusinessRules(final Map<TicketType , Long> counts) {
        final long totalTickets = counts.values().stream().mapToLong(Long::longValue).sum();
        final long adults = counts.get(TicketType.ADULT);
        //Infants sit on adults lap and don't get a seat
        final long seatless = counts.entrySet().stream()
                .filter(entry -> !properties.occupiesSeat(entry.getKey()))
                .mapToLong(Map.Entry::getValue)
                .sum();

        final int maxTickets = properties.maxTicketsPerBooking();
        if(totalTickets==0) {
            throw new InvalidBookingException("At least one ticket must be purchased");
        }
        if(totalTickets>maxTickets) {
            throw new InvalidBookingException("A maximum of " + maxTickets + " tickets can be purchased at a time");
        }
        if(adults == 0) {
            throw new InvalidBookingException("Child and infant tickets cannot be purchased without an adult ticket");
        }
        if(seatless > adults) {
            throw new InvalidBookingException("Infants sit on an adult's lap, so infants cannot outnumber adults");
        }
    }

}
