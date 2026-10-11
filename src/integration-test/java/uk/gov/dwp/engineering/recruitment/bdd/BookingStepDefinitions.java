package uk.gov.dwp.engineering.recruitment.bdd;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class BookingStepDefinitions {

    private static final String BOOKINGS_URL = "/cinema/bookings";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private SeatReservationService seatReservationService;

    private MockMvc mockMvc;

    private long accountId;

    private final List<String> ticketRequests = new ArrayList<>();

    private ResultActions response;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        clearInvocations(paymentService, seatReservationService);
    }

    @Given("I am account {long}")
    public void iAmAccount(final long accountId) { this.accountId=accountId; }

    @Given("I want {int} {word} tickets")
    public void iWantTickets(final long count, final String type) {
        ticketRequests.add("{\"type\":\"" + type + "\",\"ticketCount\":" + count + "}");
    }

    @When("I submit the booking")
    public void iSubmitTheBooking() throws Exception {
        final String body = "{\"accountId\":" + accountId + ",\"ticketRequests\":["
                + String.join(",", ticketRequests) + "]}";
        response = submit(body);
    }

    @When("I submit a malformed booking request")
    public void iSubmitAMalformedBookingRequest() throws Exception {
        response = submit("{malformed");
    }

    @Then("the booking is created")
    public void theBookingIsCreated() throws Exception {
        response.andExpect(status().isCreated());
    }

    @Then("the total cost is {bigdecimal}")
    public void theTotalCostIs(final BigDecimal cost) throws Exception {
        response.andExpect(jsonPath("$.totalCost").value(cost.doubleValue()));
    }

    @Then("{long} seats are reserved")
    public void seatsAreReserved(final long seats) throws Exception {
        response.andExpect(jsonPath("$.seatCount").value(seats));
        verify(seatReservationService).reserveSeats(accountId, seats);
    }

    @Then("account {long} is charged {bigdecimal}")
    public void accountIsCharged(final long account, final BigDecimal amount) {
        verify(paymentService).debitAccount(accountId, amount);
    }

    @Then("the booking is rejected with the message {string}")
    public void theBookingIsRejectedWithTheMessage(final String message) throws Exception {
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(message));
    }

    @Then("the booking is rejected as a bad request")
    public void theBookingIsRejectedAsBadRequest() throws Exception {
        response.andExpect(status().isBadRequest());
    }

    @Then("no payment is taken and no seats are reserved")
    public void npPaymentIsTakenAndNoSeatsAreReserved() {
        verifyNoInteractions(paymentService, seatReservationService);
    }

    private ResultActions submit(final String json) throws Exception {
        return mockMvc.perform(
                post(BOOKINGS_URL).contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
