package uk.gov.dwp.engineering.recruitment.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Result of a successful booking")
public record BookingConfirmation(
        @Schema(description = "Account that had requested the booking and was charged", example = "1") Long accountId,
        @Schema(description = "No of reserved seats; infants sit on adult's lap and don't get a seat", example = "3")
        long seatCount,
        @Schema(description = "Total amount debited, to 2 decimal places", example = "69.48") BigDecimal totalCost) {

    // Retained old constructor for backward compatibility
    public BookingConfirmation(final Long accountId) {
        this(accountId, 0L, BigDecimal.ZERO);
    }
}
