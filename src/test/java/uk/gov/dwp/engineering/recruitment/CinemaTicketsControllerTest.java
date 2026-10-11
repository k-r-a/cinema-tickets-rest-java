package uk.gov.dwp.engineering.recruitment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.ResultActions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import uk.gov.dwp.engineering.recruitment.config.CinemaProperties;
import uk.gov.dwp.engineering.recruitment.config.CinemaProperties.Booking;
import uk.gov.dwp.engineering.recruitment.config.CinemaProperties.Category;
import uk.gov.dwp.engineering.recruitment.config.CinemaProperties.Tickets;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;
import uk.gov.dwp.engineering.recruitment.exception.RestExceptionHandler;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;



/** Web-layer tests for controller, service and exception handler where third-party services are mocked */
@ExtendWith(MockitoExtension.class)
class CinemaTicketsControllerTest {

    private static final String BOOKINGS_URL = "/cinema/bookings";

    @Mock
    private PaymentService paymentService;

    @Mock
    private SeatReservationService seatReservationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(new CinemaTicketsServiceImpl(paymentService,seatReservationService,
                new CinemaProperties(new Tickets(Map.of(
                    TicketType.INFANT, new Category(new BigDecimal("0.00"), false),
                    TicketType.CHILD, new Category(new BigDecimal("17.50"), true),
                    TicketType.ADULT, new Category(new BigDecimal("25.99"), true))),
                    new Booking(25))));
    }

    private static MockMvc buildMockMvc(final CinemaTicketsService service) {
        return MockMvcBuilders.standaloneSetup(new CinemaTicketsController(service))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    private ResultActions postBooking(final String json) throws Exception {
        return mockMvc.perform(post(BOOKINGS_URL).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String booking(final String accountId, final String... ticketRequests) {
        return "{\"accountId\":" + accountId + ",\"ticketRequests\":["
                + String.join(",", ticketRequests) + "]}";
    }

    private static String tickets(final String type, final int count) {
        return "{\"type\":\"" + type + "\",\"ticketCount\":" + count + "}";
    }

    // Also tests that Infants are free and get no seat
    // Also tests Maximum Tickets can be successfully booked
    @Test
    void givenValidBooking_whenPost_thenCreatedWithConfirmationAndThirdPartiesCalled() throws Exception {
        postBooking(booking("7",tickets("ADULT",15), tickets("CHILD", 5),
                tickets("INFANT", 5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(7))
                .andExpect(jsonPath("$.seatCount").value(20L))
                .andExpect(jsonPath("$.totalCost").value(477.35));

        verify(paymentService).debitAccount(7L, new BigDecimal("477.35"));
        verify(seatReservationService).reserveSeats(7L, 20L);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidBookings")
    void givenInvalidBooking_whenPost_thenBadRequestNoThirdPartyCall(final String description, final String accountId,
            final String ticketSpec, final String expectedMessage) throws Exception {
        final String[] ticketRequests = Arrays.stream(ticketSpec.split(","))
                .map(entry -> entry.trim().split(":"))
                .map(parts -> tickets(parts[0], Integer.parseInt(parts[1])))
                .toArray(String[]::new);

        postBooking(booking(accountId.trim(), ticketRequests))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(expectedMessage.trim()));

        verifyNoInteractions(paymentService, seatReservationService);
    }

    static Stream<Arguments> invalidBookings() {
        return Stream.of(
                Arguments.of("more than 25 tickets", "7", "ADULT:26",
                        "A maximum of 25 tickets can be purchased at a time"),
                Arguments.of("infants count towards the maximum", "7", "ADULT:13,INFANT:13",
                        "A maximum of 25 tickets can be purchased at a time"),
                Arguments.of("child without adult" , "7", "CHILD:1",
                        "Child and infant tickets cannot be purchased without an adult ticket"),
                Arguments.of("infant without adult" , "7", "INFANT:1",
                        "Child and infant tickets cannot be purchased without an adult ticket"),
                Arguments.of("more infants than adults", "7", "ADULT:1,INFANT:2",
                        "Infants sit on an adult's lap, so infants cannot outnumber adults"),
                Arguments.of("zero tickets", "7", "ADULT:0", "At least one ticket must be purchased"),
                Arguments.of("negative count", "7", "ADULT:1,CHILD:-1", "Ticket count must not be negative"),
                Arguments.of("zero account id", "0", "ADULT:1", "Account id must be greater than zero"),
                Arguments.of("negative account id", "-5", "ADULT:1",
                        "Account id must be greater than zero"));
    }

    @Test
    void givenMissingAccountId_whenPost_thenBadRequest() throws Exception {
        postBooking("{\"ticketRequests\":[" + tickets("ADULT", 1) + "]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Account id must be greater than zero"));

        verifyNoInteractions(paymentService, seatReservationService);
    }

    @Test
    void givenMissingTicketRequests_whenPost_thenBadRequest() throws Exception {
        postBooking("{\"accountId\":7}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("At least one ticket request is required"));

        verifyNoInteractions(paymentService, seatReservationService);
    }

    @Test
    void givenEmptyTicketRequests_whenPost_thenBadRequest() throws Exception {
        postBooking(booking("7"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("At least one ticket request is required"));

        verifyNoInteractions(paymentService, seatReservationService);
    }

    @Test
    void givenMissingTicketType_whenPost_thenBadRequest() throws Exception {
        postBooking(booking("7", "{\"ticketCount\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("Each ticket request must specify a ticket type"));

        verifyNoInteractions(paymentService, seatReservationService);
    }

    @Test
    void givenUnknownTicketType_whenPost_thenBadRequest() throws Exception {
        postBooking(booking("7",tickets("SENIOR", 1)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(paymentService, seatReservationService);
    }

    @Test
    void givenNonNumericTicketCount_whenPost_thenBadRequest() throws Exception {
        postBooking("{\"accountId\":7,\"ticketRequests\":[{\"type\":\"ADULT\",\"ticketCount\":\"x\"}]}")
                .andExpect(status().isBadRequest());
        verifyNoInteractions(paymentService, seatReservationService);
    }

    @Test
    void givenMalformedJson_whenPost_thenBadRequest() throws Exception {
        postBooking("{malformed").andExpect(status().isBadRequest());
        verifyNoInteractions(paymentService, seatReservationService);
    }

    @Test
    void givenEmptyBody_whenPost_thenBadRequest() throws Exception {
        mockMvc.perform(post(BOOKINGS_URL).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
        verifyNoInteractions(paymentService, seatReservationService);
    }

    @Test
    void givenGetRequest_whenBookings_thenMethodNotAllowed() throws Exception {
        mockMvc.perform(get(BOOKINGS_URL)).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void givenUnexpectedFailure_whenPost_thenHideUnknownInternalErrorDetails()
            throws Exception {
        final CinemaTicketsService failingService = mock(CinemaTicketsService.class);
        when(failingService.purchaseTickets(any(), any(TicketRequest[].class)))
                .thenThrow(new IllegalStateException("Unknown internal error details to hide"));

        buildMockMvc(failingService)
                .perform(post(BOOKINGS_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(booking("7", tickets("ADULT", 1))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }
}
