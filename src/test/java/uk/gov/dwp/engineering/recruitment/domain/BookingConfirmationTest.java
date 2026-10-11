package uk.gov.dwp.engineering.recruitment.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class BookingConfirmationTest {

  @Test
  void givenBookingConfirmation_whenGetAccount_thenExpectedValueReturned() {
    final BookingConfirmation bookingConfirmation = new BookingConfirmation(99L);
    assertEquals(99L, bookingConfirmation.accountId());
  }

  @Test
  void givenAccountOnlyConstructor_whenGetSeatCountAndCost_thenZeroReturned() {
    final BookingConfirmation bookingConfirmation = new BookingConfirmation(99L);
    assertEquals(0L, bookingConfirmation.seatCount());
    assertEquals(BigDecimal.ZERO, bookingConfirmation.totalCost());
  }

  @Test
  void givenFullBookingConfirmation_whenGetValues_thenExpectedValuesReturned() {
    final BookingConfirmation bookingConfirmation = new BookingConfirmation(99L, 3L,
            new BigDecimal("69.48"));
    assertEquals(99L, bookingConfirmation.accountId());
    assertEquals(3L, bookingConfirmation.seatCount());
    assertEquals(new BigDecimal("69.48"), bookingConfirmation.totalCost());
  }
}
