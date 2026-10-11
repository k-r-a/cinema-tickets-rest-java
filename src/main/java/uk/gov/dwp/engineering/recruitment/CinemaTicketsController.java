package uk.gov.dwp.engineering.recruitment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.dwp.engineering.recruitment.domain.Booking;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;

@RestController
@RequestMapping("/cinema")
@Tag(name = "bookings", description = "Endpoints for cinema ticket bookings")
public class CinemaTicketsController {

    /**
     * The Ticket Booking Service.
     */
    private final CinemaTicketsService cinemaTicketsService;

    public CinemaTicketsController(final CinemaTicketsService cinemaTicketsService) {
        this.cinemaTicketsService = cinemaTicketsService;
    }

    /**
     * Make booking endpoint handler.
     *
     * @return ResponseEntity containing the result of the booking.
     */
    @Operation(summary = "Make cinema ticket bookings",
            description = "Validates the booking request, processes payment and reserves seats. Rules: the " +
                    "account id must be greater than 0; at least one ticket is required and no more than the " +
                    "configured maximum per booking; child and infant tickets need at least one adult ticket; each " +
                    "infant sits on an adult's lap, so infants don't get a seat and they cannot outnumber adults.")
    @ApiResponse(responseCode = "201", description = "Successful booking")
    @ApiResponse(responseCode = "400",
            description = "Booking rejected : invalid account id, missing or malformed ticket requests, negative " +
                    "ticket count, no tickets, too many tickets, no adult, or infants outnumber adults. " +
                    "The reason is in the 'detail' field.",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(value = "/bookings", consumes = "application/json", produces = "application/json")
    public ResponseEntity<BookingConfirmation> makeBooking(@RequestBody Booking ticketBooking) {
        return new ResponseEntity<>(cinemaTicketsService.purchaseTickets(ticketBooking.accountId(),
                ticketBooking.ticketRequests()), HttpStatus.CREATED);
    }

}
