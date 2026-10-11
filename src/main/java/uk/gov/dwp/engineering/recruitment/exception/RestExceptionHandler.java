package uk.gov.dwp.engineering.recruitment.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(RestExceptionHandler.class);

  @ExceptionHandler(InvalidBookingException.class)
  protected ProblemDetail handleInvalidBookingException(final InvalidBookingException ex) {
    LOG.warn("Booking rejected: {}", ex.getMessage());
    final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problemDetail.setDetail(ex.getMessage());
    return problemDetail;
  }

  // Final resort to handle any unexpected errors
  @ExceptionHandler(Exception.class)
  protected ProblemDetail handleUnexpectedException(final Exception ex) {
    LOG.error("Unexpected error handling ", ex);
    return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred");
  }
}
