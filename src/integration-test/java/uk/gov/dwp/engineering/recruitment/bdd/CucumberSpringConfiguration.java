package uk.gov.dwp.engineering.recruitment.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.dwp.engineering.recruitment.CinemaTicketsApplication;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

/** Boots the full application once for all scenarios; third-party beans are spied on. */
@CucumberContextConfiguration
@SpringBootTest(classes = CinemaTicketsApplication.class)
public class CucumberSpringConfiguration {

    @MockitoSpyBean
    PaymentService paymentService;

    @MockitoSpyBean
    SeatReservationService seatReservationService;
}
