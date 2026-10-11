package uk.gov.dwp.engineering.recruitment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.CHILD;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.engineering.recruitment.config.CinemaProperties;
import static uk.gov.dwp.engineering.recruitment.config.CinemaProperties.Category;
import static uk.gov.dwp.engineering.recruitment.config.CinemaProperties.Booking;
import static uk.gov.dwp.engineering.recruitment.config.CinemaProperties.Tickets;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

@ExtendWith(MockitoExtension.class)
class CinemaTicketsServiceImplTest {

    private static final Long ACCOUNT_ID = 42L;

    @Mock
    private PaymentService paymentService;

    @Mock
    private SeatReservationService seatReservationService;

    private CinemaTicketsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CinemaTicketsServiceImpl(paymentService, seatReservationService,
                properties("0.00", "17.50", "25.99", 25));
    }

    private static CinemaProperties properties(final String infantPrice, final String childPrice, final String adultPrice, final int maxTickets) {
        return new CinemaProperties(new Tickets(Map.of(
                INFANT, new Category(new BigDecimal(infantPrice), false),
                CHILD, new Category(new BigDecimal(childPrice), true),
                ADULT, new Category(new BigDecimal(adultPrice), true))), new Booking(maxTickets));
    }

    // Also tests booking success when exactly max tickets are requested
    // Also tests that custom prices are applied to calculate cost
    // Also tests that seats are reserved for Adults and Child
    // Also tests that Infants don't get a seat
    // Also tests booking success when equal adults and infants
    @Test
    void givenMixedBooking_whenPurchaseMaxTickets_thenConfirmationHasCorrectCostAndSeats() {
        final CinemaTicketsServiceImpl mixedBookingForMaxTicketsService = new CinemaTicketsServiceImpl(
                paymentService, seatReservationService,properties("0", "12.50", "25.00",
                20));

        final BookingConfirmation confirmation = mixedBookingForMaxTicketsService.purchaseTickets(ACCOUNT_ID,
                new TicketRequest(ADULT, 8), new TicketRequest(CHILD, 4),
                new TicketRequest(INFANT, 8));

        assertConfirmation(confirmation, 12, new BigDecimal("250.00"));
    }

    // Also tests that zero is allowed in ticket count as long as total number of tickets is not zero
    @Test
    void givenRepeatedTicketTypes_whenPurchase_thenCountsAreAggregated() {
        final BookingConfirmation confirmation = service.purchaseTickets(ACCOUNT_ID,
                new TicketRequest(ADULT, 1), new TicketRequest(CHILD, 1),
                new TicketRequest(ADULT, 1), new TicketRequest(CHILD, 1),
                new TicketRequest(ADULT, 0),new TicketRequest(INFANT, 1));

        assertConfirmation(confirmation, 4, new BigDecimal("86.98"));
    }

    // Also tests that infants count towards max tickets
    @Test
    void givenCustomMaxTickets_whenPurchaseExceeds_thenExceptionWithConfiguredLimitInMessage() {
        final CinemaTicketsServiceImpl smallMaxTicketsService = new CinemaTicketsServiceImpl(
            paymentService, seatReservationService,properties("0", "1", "2", 4));
        final InvalidBookingException  exception = assertThrows(InvalidBookingException.class, () -> {
            smallMaxTicketsService.purchaseTickets(ACCOUNT_ID, new TicketRequest(ADULT, 3),
                    new TicketRequest(CHILD, 1), new TicketRequest(INFANT, 1));
        });
        assertEquals("A maximum of 4 tickets can be purchased at a time", exception.getMessage());
        verifyNoInteractions(paymentService, seatReservationService);
    }

    @Test
    void givenTotalZeroCounts_whenPurchase_thenRejected() {
        assertRejected(ACCOUNT_ID, "At least one ticket must be purchased",
                new TicketRequest(ADULT, 0));
    }

    @Test
    void givenChildWithoutAdult_whenPurchase_thenRejected() {
        assertRejected(ACCOUNT_ID,
                "Child and infant tickets cannot be purchased without an adult ticket",
                new TicketRequest(CHILD, 1));
    }

    @Test
    void givenInfantWithoutAdult_whenPurchase_thenRejected() {
        assertRejected(ACCOUNT_ID,
                "Child and infant tickets cannot be purchased without an adult ticket",
                new TicketRequest(INFANT, 1));
    }

    @Test
    void givenMoreInfantsThanAdults_whenPurchase_thenRejected() {
        assertRejected(ACCOUNT_ID,
                "Infants sit on an adult's lap, so infants cannot outnumber adults",
                new TicketRequest(INFANT, 2), new TicketRequest(ADULT, 1));
    }

    @Test
    void givenNegativeTicketCount_whenPurchase_thenRejected() {
        assertRejected(ACCOUNT_ID,
                "Ticket count must not be negative",
                new TicketRequest(INFANT, -1), new TicketRequest(ADULT, 1));
    }

    @Test
    void givenNullTicketType_whenPurchase_thenRejected() {
        assertRejected(ACCOUNT_ID,
                "Each ticket request must specify a ticket type",
                new TicketRequest(null, 1), new TicketRequest(ADULT, 1));
    }

    @Test
    void givenNullTicketRequestEntry_whenPurchase_thenRejected() {
        assertRejected(ACCOUNT_ID,
                "Each ticket request must specify a ticket type",
                new TicketRequest(ADULT, 1), null);
    }

    @Test
    void givenNullTicketRequests_whenPurchase_thenRejected() {
        assertRejected(ACCOUNT_ID,
                "At least one ticket request is required",
                (TicketRequest[]) null);
    }

    @Test
    void givenNoTicketRequests_whenPurchase_thenRejected() {
        assertRejected(ACCOUNT_ID,
                "At least one ticket request is required");
    }

    @Test
    void givenNullAccountId_whenPurchase_thenRejected() {
        assertRejected(null, "Account id must be greater than zero",
                new TicketRequest(ADULT, 1));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void givenNonPositiveAccountId_whenPurchase_thenRejected(final long accointId) {
        assertRejected(accointId, "Account id must be greater than zero",
                new TicketRequest(ADULT, 1));
    }

    private void assertRejected(final Long accountId, final String expectedMessage, final TicketRequest... requests) {
        final InvalidBookingException exception = assertThrows(InvalidBookingException.class,
                () -> service.purchaseTickets(accountId, requests));
        assertEquals(expectedMessage, exception.getMessage());
        verifyNoInteractions(paymentService, seatReservationService);
    }

    private void assertConfirmation(final BookingConfirmation confirmation, final long expectedSeats,
                                           final BigDecimal expectedCost) {
        assertEquals(ACCOUNT_ID, confirmation.accountId());
        assertEquals(expectedSeats, confirmation.seatCount());
        assertEquals(expectedCost, confirmation.totalCost());
        verify(paymentService).debitAccount(ACCOUNT_ID, expectedCost);
        verify(seatReservationService).reserveSeats(ACCOUNT_ID, expectedSeats);
    }
}
